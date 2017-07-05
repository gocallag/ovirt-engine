package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.quota;

import org.ovirt.engine.core.common.businessentities.Permission;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.GroupedTabData;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaUserListModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

public class SubTabQuotaUserPresenter
    extends AbstractSubTabQuotaPresenter<QuotaUserListModel, SubTabQuotaUserPresenter.ViewDef,
        SubTabQuotaUserPresenter.ProxyDef> {

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.quotaUserSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabQuotaUserPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<Quota> {
    }

    @TabInfo(container = QuotaSubTabPanelPresenter.class)
    static TabData getTabData() {
        return new GroupedTabData(constants.quotaUserSubTabLabel(), 4);
    }

    @Inject
    public SubTabQuotaUserPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager, QuotaMainTabSelectedItems selectedItems,
            QuotaUserActionPanelPresenterWidget actionPanel,
            SearchableDetailModelProvider<Permission, QuotaListModel, QuotaUserListModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider, selectedItems, actionPanel,
                QuotaSubTabPanelPresenter.TYPE_SetTabContent);
    }
}
