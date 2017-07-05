package org.ovirt.engine.ui.webadmin.section.main.view.popup.macpool;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.MacPool;
import org.ovirt.engine.ui.common.MainTableHeaderlessResources;
import org.ovirt.engine.ui.common.MainTableResources;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.widget.table.SimpleActionTable;
import org.ovirt.engine.ui.common.widget.table.column.AbstractImageResourceColumn;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.common.widget.uicommon.permissions.PermissionListModelTable;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.macpool.SharedMacPoolListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.PermissionModelProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.SharedMacPoolModelProvider;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.inject.Inject;

public class SharedMacPoolView extends Composite {

    private final PermissionModelProvider<MacPool, SharedMacPoolListModel> permissionModelProvider;
    private final SharedMacPoolModelProvider sharedMacPoolModelProvider;

    private final FlowPanel container = new FlowPanel();
    private final SimpleActionTable<MacPool> macPoolTable;
    private final PermissionListModelTable<PermissionListModel<MacPool>> authorizationTable;

    private final EventBus eventBus;
    private final ClientStorage clientStorage;
    private final MainTableHeaderlessResources headerlessResources;
    private final MainTableResources tableResources;
    private final SplitLayoutPanel rootPanel;

    private static final ApplicationResources resources = AssetProvider.getResources();
    private static final ApplicationConstants constants = AssetProvider.getConstants();

    @Inject
    public SharedMacPoolView(final SharedMacPoolModelProvider sharedMacPoolModelProvider,
            final PermissionModelProvider<MacPool, SharedMacPoolListModel> permissionModelProvider,
            final EventBus eventBus,
            final ClientStorage clientStorage,
            final MainTableHeaderlessResources headerlessResources,
            final MainTableResources tableResources,
            SharedMacPoolActionPanelPresenterWidget actionPanel) {

        this.sharedMacPoolModelProvider = sharedMacPoolModelProvider;
        this.permissionModelProvider = permissionModelProvider;

        this.eventBus = eventBus;
        this.clientStorage = clientStorage;
        this.headerlessResources = headerlessResources;
        this.tableResources = tableResources;

        macPoolTable = createMacPoolTable();
        container.add(actionPanel);
        container.add(macPoolTable);
        authorizationTable = new PermissionListModelTable<>(permissionModelProvider, eventBus, null, clientStorage);
        authorizationTable.initTable();

        authorizationTable.getTable().getSelectionModel().addSelectionChangeHandler(event ->
                permissionModelProvider.setSelectedItems(authorizationTable.getTable().getSelectedItems()));

        rootPanel = createRootPanel();

        setupAuthorizationTableVisibility(false);

        initWidget(rootPanel);
    }

    private void setupAuthorizationTableVisibility(boolean visible) {
        rootPanel.clear();
        if (visible) {
            rootPanel.addSouth(authorizationTable, 150);
        }
        rootPanel.add(container);
    }

    private SplitLayoutPanel createRootPanel() {
        SplitLayoutPanel rootPanel = new SplitLayoutPanel();
        rootPanel.setHeight("470px"); //$NON-NLS-1$
        rootPanel.setWidth("100%"); //$NON-NLS-1$
        return rootPanel;
    }

    private SimpleActionTable<MacPool> createMacPoolTable() {

        final SimpleActionTable<MacPool> macPoolTable =
                new SimpleActionTable<>(sharedMacPoolModelProvider,
                        headerlessResources,
                        tableResources,
                        eventBus,
                        clientStorage);
        macPoolTable.addColumn(new AbstractImageResourceColumn<MacPool>() {

            @Override
            public ImageResource getValue(MacPool macPool) {
                return macPool.isDefaultPool() ? resources.lockImage() : null;
            }
        }, constants.empty(), "20px"); //$NON-NLS-1$
        macPoolTable.addColumn(new AbstractTextColumn<MacPool>() {

            @Override
            public String getValue(MacPool macPool) {
                return macPool.getName();
            }
        }, constants.configureMacPoolNameColumn(), "100px"); //$NON-NLS-1$
        macPoolTable.addColumn(new AbstractTextColumn<MacPool>() {

            @Override
            public String getValue(MacPool macPool) {
                return macPool.getDescription();
            }
        }, constants.configureMacPoolDescriptionColumn(), "300px"); //$NON-NLS-1$

        macPoolTable.getSelectionModel().addSelectionChangeHandler(event -> {
            final List<MacPool> selectedItems = macPoolTable.getSelectedItems();
            sharedMacPoolModelProvider.setSelectedItems(selectedItems);

            final PermissionListModel<MacPool> model = permissionModelProvider.getModel();

            if (selectedItems.size() == 1) {
                model.setEntity(selectedItems.get(0));
                setupAuthorizationTableVisibility(true);
            } else {
                model.setEntity(null);
                setupAuthorizationTableVisibility(false);
            }
        });

        return macPoolTable;
    }
}
