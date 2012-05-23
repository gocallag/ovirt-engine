package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.action.SetupNetworksParameters;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.NetworkUtils;

public class SetupNetworksHelper {
    private SetupNetworksParameters params;
    private Guid vdsGroupId;
    private List<VdcBllMessages> violations = new ArrayList<VdcBllMessages>();
    private Map<String, VdsNetworkInterface> existingIfaces;
    private Map<String, network> existingClusterNetworks;

    private List<network> networks;
    private List<String> unmanagedNetworks = new ArrayList<String>();
    private List<String> removeNetworks;
    private List<VdsNetworkInterface> bonds;
    private List<VdsNetworkInterface> removedBonds;

    public SetupNetworksHelper(SetupNetworksParameters parameters, Guid vdsGroupId) {
        params = parameters;
        this.vdsGroupId = vdsGroupId;
    }

    /**
     * validate and extract data from the list of interfaces sent. The general flow is:
     * <ul>
     * <li>create mapping of existing the current topology - interfaces and logical networks.
     * <li>create maps for networks bonds and bonds-slaves.
     * <li>iterate over the interfaces and extract network/bond/slave info as we go.
     * <li>validate the extracted information by using the pre-build mappings of the current topology.
     * <li>store and encapsulate the extracted lists to later be fetched by the calling command.
     * <li>error messages are aggregated
     * </ul>
     * TODO add fail-fast to exist on the first validation error.
     *
     * @return List of violations encountered (if none, list is empty).
     */
    public List<VdcBllMessages> validate() {
        Set<String> ifaceNames = new HashSet<String>();

        // key = bond name, value = interface
        Map<String, VdsNetworkInterface> bonds = new HashMap<String, VdsNetworkInterface>();

        // key = master bond name, value = list of interfaces
        Map<String, List<VdsNetworkInterface>> bondSlaves = new HashMap<String, List<VdsNetworkInterface>>();

        // key = network name, vale = interface
        Map<String, network> networks = new HashMap<String, network>();

        for (VdsNetworkInterface iface : params.getInterfaces()) {
            String name = iface.getName();
            String networkName = iface.getNetworkName();
            String bondName = iface.getBondName();
            boolean bonded = isBond(iface);

            if (ifaceNames.contains(name)) {
                violations.add(VdcBllMessages.NETWORK_INTERFACE_NAME_ALREADY_IN_USE);
                continue;
            } else {
                ifaceNames.add(name);
            }

            // if its a bond extract to bonds map
            if (bonded) {
                extractBond(bonds, iface, name);
            } else {
                if (StringUtils.isNotBlank(bondName)) {
                    extractBondSlave(bondSlaves, iface, bondName);
                }

                // validate the nic exists on host
                if (!getExistingIfaces().containsKey(NetworkUtils.StripVlan(name))) {
                    violations.add(VdcBllMessages.NETWORK_INTERFACE_NOT_EXISTS);
                }
            }

            // validate and extract to network map
            if (violations.isEmpty() && StringUtils.isNotBlank(networkName)) {
                extractNetwork(networks, iface, networkName);
            }
        }

        validateBonds(bonds, bondSlaves);

        this.networks = new ArrayList<network>(networks.values());
        extractRemoveNetworks(networks.keySet(), getExisitingHostNetworkNames());
        this.bonds = new ArrayList<VdsNetworkInterface>(bonds.values());
        this.removedBonds = extractRemovedBonds(bonds);

        return violations;
    }

    private Map<String, network> getExistingClusterNetworks() {
        if (existingClusterNetworks == null) {
            existingClusterNetworks = Entities.entitiesByName(
                    getDbFacade().getNetworkDAO().getAllForCluster(vdsGroupId));
        }

        return existingClusterNetworks;
    }

    private Map<String, VdsNetworkInterface> getExistingIfaces() {
        if (existingIfaces == null) {
            existingIfaces = Entities.entitiesByName(
                    getDbFacade().getInterfaceDAO().getAllInterfacesForVds(params.getVdsId()));
        }

        return existingIfaces;
    }

    protected DbFacade getDbFacade() {
        return DbFacade.getInstance();
    }

    private List<String> getExisitingHostNetworkNames() {
        List<String> list = new ArrayList<String>(getExistingIfaces().size());
        for (VdsNetworkInterface iface : getExistingIfaces().values()) {
            if (StringUtils.isNotBlank(iface.getNetworkName())) {
                list.add(iface.getNetworkName());
            }
        }

        return list;
    }

    /**
     * extracting a network is done by matching the desired network name with the network details from db on
     * clusterNetworksMap. The desired network is just a key and actual network configuration is taken from the db
     * entity.
     *
     * @param networks
     *            map of aggregated networks to add
     * @param iface
     *            current iterated interface
     * @param networkName
     *            the current network name of iface
     */
    private void extractNetwork(Map<String, network> networks,
            VdsNetworkInterface iface,
            String networkName) {

        // check if network exists on cluster
        if (getExistingClusterNetworks().containsKey(networkName)) {

            // prevent attaching 2 interfaces to 1 network
            if (networks.containsKey(networkName)) {
                violations.add(VdcBllMessages.NETWROK_ALREADY_ATTACHED_TO_INTERFACE);
            } else {
                networks.put(networkName, getExistingClusterNetworks().get(networkName));
            }

            // Interface must exist, it was checked before and we can't reach here if it does'nt exist already.
        } else if (networkName.equals(getExistingIfaces().get(iface.getName()).getNetworkName())) {
            unmanagedNetworks.add(networkName);
        } else {
            violations.add(VdcBllMessages.NETWORK_NOT_EXISTS_IN_CURRENT_CLUSTER);
        }
    }

    private boolean isBond(VdsNetworkInterface iface) {
        return Boolean.TRUE.equals(iface.getBonded());
    }

    /**
     * build mapping of the bond name - > list of slaves. slaves are interfaces with a pointer to the master bond by
     * bondName.
     *
     * @param bondSlaves
     * @param iface
     * @param bondName
     */
    protected void extractBondSlave(Map<String, List<VdsNetworkInterface>> bondSlaves,
            VdsNetworkInterface iface,
            String bondName) {
        List<VdsNetworkInterface> value = new ArrayList<VdsNetworkInterface>();
        value.add(iface);
        if (bondSlaves.containsKey(bondName)) {
            value.addAll(bondSlaves.get(bondName));
        }

        bondSlaves.put(bondName, value);
    }

    protected void extractBond(Map<String, VdsNetworkInterface> bonds, VdsNetworkInterface iface, String name) {
        if (StringUtils.isBlank(iface.getNetworkName())) {
            violations.add(VdcBllMessages.NETWROK_NOT_EXISTS);
        } else {
            if (bonds.containsKey(name)) {
                violations.add(VdcBllMessages.NETWORK_BOND_NAME_EXISTS);
            } else {
                bonds.put(name, iface);
            }
        }
    }

    protected List<VdsNetworkInterface> extractRemovedBonds(Map<String, VdsNetworkInterface> bonds) {
        List<VdsNetworkInterface> removedBonds = new ArrayList<VdsNetworkInterface>();
        for (Entry<String, VdsNetworkInterface> e : getExistingIfaces().entrySet()) {
            if (isBond(e.getValue()) && !bonds.containsKey(e.getKey())) {
                removedBonds.add(e.getValue());
            }
        }

        return removedBonds;
    }

    protected boolean validateBonds(Map<String, VdsNetworkInterface> bonds,
            Map<String, List<VdsNetworkInterface>> bondSlaves) {
        boolean returnValue = true;
        for (String bondName : bonds.keySet()) {
            if (bondSlaves.containsKey(bondName) && bondSlaves.get(bondName).size() < 2) {
                returnValue = false;
                violations.add(VdcBllMessages.NETWORK_BOND_PARAMETERS_INVALID);
            }
        }

        return returnValue;
    }

    /**
     * Calculate the networks that should be removed - If the network was attached to a NIC and is no longer attached to
     * it, then it will be removed.
     *
     * @param networkNames
     *            The names of managed networks which are still attached to a NIC.
     * @param exisitingHostNetworksNames
     *            The names of networks that were attached to NICs before the changes.
     */
    protected void extractRemoveNetworks(Set<String> networkNames,
            List<String> exisitingHostNetworksNames) {
        removeNetworks = new ArrayList<String>();

        for (String net : exisitingHostNetworksNames) {
            if (!networkNames.contains(net) && !unmanagedNetworks.contains(net)) {
                removeNetworks.add(net);
            }
        }
    }

    public List<VdcBllMessages> getViolations() {
        return violations;
    }

    public List<network> getNetworks() {
        return networks;
    }

    public List<String> getRemoveNetworks() {
        return removeNetworks;
    }

    public List<VdsNetworkInterface> getBonds() {
        return bonds;
    }

    public List<VdsNetworkInterface> getRemovedBonds() {
        return removedBonds;
    }
}
