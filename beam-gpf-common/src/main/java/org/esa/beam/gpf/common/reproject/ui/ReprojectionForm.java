package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.DemSelector;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Marco
 * Date: 16.08.2009
 */
class ReprojectionForm extends JTabbedPane {

    private static final String[] RESAMPLING_IDENTIFIER = {"Nearest", "Bilinear", "Bicubic"};

    private final boolean orthoMode;
    private final AppContext appContext;

    private SourceProductSelector sourceProductSelector;
    private TargetProductSelector targetProductSelector;
    private JComboBox resampleComboBox;
    private JCheckBox includeTPcheck;

    private Product sourceProduct;
    private DemSelector demSelector;
    private ValueContainer outputParameterContainer;
    private ButtonModel resolutionBtnModel;

    private CrsForm crsForm;

    ReprojectionForm(TargetProductSelector targetProductSelector, boolean orthorectify, AppContext appContext) {
        this.targetProductSelector = targetProductSelector;
        orthoMode = orthorectify;
        this.appContext = appContext;
        sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");
        createUI();
    }

    public Map<String, Object> getParameterMap() {
        Map<String, Object> parameterMap = new HashMap<String, Object>(5);
        parameterMap.put("resamplingName", resampleComboBox.getSelectedItem().toString());
        parameterMap.put("includeTiePointGrids", includeTPcheck.isSelected());
        try {
            if (!crsForm.isCollocate()) {
                final CoordinateReferenceSystem crs = crsForm.getCrs();
                parameterMap.put("wkt", crs.toWKT());
            }
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        }
        if (orthoMode) {
            parameterMap.put("orthorectify", orthoMode);
            if (demSelector.isUsingExternalDem()) {
                parameterMap.put("elevationModelName", demSelector.getDemName());
            } else {
                parameterMap.put("elevationModelName", null);
            }
        }

        if (outputParameterContainer != null) {
            parameterMap.put("referencePixelX", outputParameterContainer.getValue("referencePixelX"));
            parameterMap.put("referencePixelY", outputParameterContainer.getValue("referencePixelY"));
            parameterMap.put("easting", outputParameterContainer.getValue("easting"));
            parameterMap.put("northing", outputParameterContainer.getValue("northing"));
            parameterMap.put("orientation", outputParameterContainer.getValue("orientation"));
            parameterMap.put("pixelSizeX", outputParameterContainer.getValue("pixelSizeX"));
            parameterMap.put("pixelSizeY", outputParameterContainer.getValue("pixelSizeY"));
            parameterMap.put("width", outputParameterContainer.getValue("width"));
            parameterMap.put("height", outputParameterContainer.getValue("height"));
            parameterMap.put("noDataValue", outputParameterContainer.getValue("noData"));
        }
        return parameterMap;
    }

    public Map<String, Product> getProductMap() {
        final Map<String, Product> productMap = new HashMap<String, Product>(5);
        productMap.put("source", sourceProduct);
        if (crsForm.isCollocate()) {
            productMap.put("collocate", crsForm.getCollocationProduct());
        }
        return productMap;
    }

    public void prepareShow() {
        sourceProductSelector.initProducts();
        crsForm.prepareShow();
    }

    public void prepareHide() {
        sourceProductSelector.releaseProducts();
        crsForm.prepareHide();
    }

    private void createUI() {
        addTab("I/O Parameter", createIOPanel());
        addTab("Reprojection Parameter", createParameterPanel());
    }

    private JPanel createIOPanel() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(3, 3);

        final JPanel ioPanel = new JPanel(tableLayout);
        ioPanel.add(createSourceProductPanel());
        ioPanel.add(targetProductSelector.createDefaultPanel());
        ioPanel.add(tableLayout.createVerticalSpacer());
        return ioPanel;
    }

    private JPanel createParameterPanel() {
        final JPanel parameterPanel = new JPanel();
        final TableLayout layout = new TableLayout(1);
        layout.setTablePadding(4, 4);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(1.0);
        parameterPanel.setLayout(layout);

        crsForm = new CrsForm(appContext);
        parameterPanel.add(crsForm);
        if (orthoMode) {
            demSelector = new DemSelector();
            parameterPanel.add(demSelector);
        }
        parameterPanel.add(createOuputSettingsPanel());
        return parameterPanel;
    }

    private JPanel createOuputSettingsPanel() {
        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnFill(0, TableLayout.Fill.NONE);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setColumnPadding(0, new Insets(4, 4, 4, 20));
        tableLayout.setColumnWeightX(0, 0.0);
        tableLayout.setColumnWeightX(1, 0.0);
        tableLayout.setColumnWeightX(2, 1.0);
        tableLayout.setCellColspan(0, 1, 2);
        tableLayout.setCellPadding(1, 0, new Insets(4, 24, 4, 20));

        final JPanel outputSettingsPanel = new JPanel(tableLayout);
        outputSettingsPanel.setBorder(BorderFactory.createTitledBorder("Output Settings"));

        final JCheckBox preserveResolutionCheckBox = new JCheckBox("Preserve resolution");
        resolutionBtnModel = preserveResolutionCheckBox.getModel();
        outputSettingsPanel.add(preserveResolutionCheckBox);
        includeTPcheck = new JCheckBox("Reproject tie-point grids", true);
        outputSettingsPanel.add(includeTPcheck);

        final JButton outputParamBtn = new JButton("Output Parameter ...");
        outputParamBtn.addActionListener(new OutputParamActionListener());
        outputSettingsPanel.add(outputParamBtn);
        outputSettingsPanel.add(new JLabel("No-data value:"));
        outputSettingsPanel.add(new JTextField());

        outputSettingsPanel.add(new JPanel());
        outputSettingsPanel.add(new JLabel("Resampling method:"));
        resampleComboBox = new JComboBox(RESAMPLING_IDENTIFIER);
        resampleComboBox.setPrototypeDisplayValue(RESAMPLING_IDENTIFIER[0]);
        outputSettingsPanel.add(resampleComboBox);

        return outputSettingsPanel;
    }

    private JPanel createSourceProductPanel() {
        final JPanel panel = sourceProductSelector.createDefaultPanel();
        sourceProductSelector.getProductNameLabel().setText("Name:");
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        sourceProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                sourceProduct = sourceProductSelector.getSelectedProduct();
                updateTargetProductName(sourceProduct);
                crsForm.setSourceProduct(sourceProduct);
            }
        });
        return panel;
    }

    private void updateTargetProductName(Product selectedProduct) {
        final TargetProductSelectorModel selectorModel = targetProductSelector.getModel();
        if (selectedProduct != null) {
            final String productName = MessageFormat.format("{0}_reprojected", selectedProduct.getName());
            selectorModel.setProductName(productName);
        } else if (selectorModel.getProductName() == null) {
            selectorModel.setProductName("reprojected");
        }
    }

    private class OutputParamActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (sourceProduct == null) {
                    appContext.handleError("Please select a product to project.\n", new IllegalStateException());
                    return;
                }
                final CoordinateReferenceSystem crs = crsForm.getCrs();
                if (crs == null) {
                    appContext.handleError("Please specify a 'Coordinate Reference System' first.\n",
                                           new IllegalStateException());
                    return;
                }
                final OutputSizeFormModel formModel = new OutputSizeFormModel(sourceProduct, crs);
                final OutputSizeForm form = new OutputSizeForm(formModel);
                final ModalDialog modalDialog = new ModalDialog(appContext.getApplicationWindow(),
                                                                "Output Parameters",
                                                                ModalDialog.ID_OK_CANCEL, null);
                modalDialog.setContent(form);
                if (modalDialog.show() == ModalDialog.ID_OK) {
                    outputParameterContainer = formModel.getValueContainer();
                }
            } catch (Exception fe) {
                appContext.handleError("Could not create a 'Coordinate Reference System'.\n" +
                                       fe.getMessage(), fe);
            }
        }
    }
}
