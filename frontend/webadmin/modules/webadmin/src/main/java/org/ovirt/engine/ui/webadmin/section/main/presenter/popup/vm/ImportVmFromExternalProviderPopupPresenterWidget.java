package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmFromExternalProviderModel;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class ImportVmFromExternalProviderPopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<ImportVmFromExternalProviderModel, ImportVmFromExternalProviderPopupPresenterWidget.ViewDef> {

    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<ImportVmFromExternalProviderModel> {
    }

    @Inject
    public ImportVmFromExternalProviderPopupPresenterWidget(EventBus eventBus, ViewDef view) {
        super(eventBus, view);
    }
}
