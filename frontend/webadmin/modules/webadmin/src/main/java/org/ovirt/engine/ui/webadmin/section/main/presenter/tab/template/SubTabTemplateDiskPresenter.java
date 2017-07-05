package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.template;

import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.uicommon.model.GroupedTabData;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateDiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
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

public class SubTabTemplateDiskPresenter
    extends AbstractSubTabTemplatePresenter<TemplateDiskListModel, SubTabTemplateDiskPresenter.ViewDef,
        SubTabTemplateDiskPresenter.ProxyDef> {

    private static final ApplicationConstants constants = AssetProvider.getConstants();

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.templateDiskSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabTemplateDiskPresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<VmTemplate> {
    }

    @TabInfo(container = TemplateSubTabPanelPresenter.class)
    static TabData getTabData() {
        return new GroupedTabData(constants.templateDiskSubTabLabel(), 3);
    }

    @Inject
    public SubTabTemplateDiskPresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager, TemplateMainTabSelectedItems selectedItems,
            TemplateDiskActionPanelPresenterWidget actionPanel,
            SearchableDetailModelProvider<DiskModel, TemplateListModel, TemplateDiskListModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider, selectedItems, actionPanel,
                TemplateSubTabPanelPresenter.TYPE_SetTabContent);
    }
}
