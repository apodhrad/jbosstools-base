/*************************************************************************************
 * Copyright (c) 2008-2010 JBoss by Red Hat and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.runtime.ui.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.runtime.core.model.IRuntimeDetector;
import org.jboss.tools.runtime.core.model.RuntimePath;
import org.jboss.tools.runtime.core.model.ServerDefinition;
import org.jboss.tools.runtime.ui.RuntimeContentProvider;
import org.jboss.tools.runtime.ui.RuntimeLabelProvider;
import org.jboss.tools.runtime.ui.RuntimeUIActivator;
import org.jboss.tools.runtime.ui.dialogs.AutoResizeTableLayout;
import org.jboss.tools.runtime.ui.dialogs.EditRuntimePathDialog;
import org.jboss.tools.runtime.ui.dialogs.RuntimePathEditingSupport;

/**
 * @author snjeza
 * 
 */
public class RuntimePreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static String ID = "org.jboss.tools.runtime.preferences.RuntimePreferencePage";
	private List<RuntimePath> runtimePaths = new ArrayList<RuntimePath>();
	private Image checkboxOn;
	private Image checkboxOff;
	private Image errorIcon;
	private TableViewer runtimePathViewer;
	private RuntimePath runtimePath;
	private Set<IRuntimeDetector> runtimeDetectors;
	private TableViewer detectorViewer;
	private Button searchButton;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		//noDefaultAndApplyButton();
		initializeDialogUnits(parent);
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		
		composite.setLayout(layout);
		
		Group pathsGroup = createGroup(composite,1);
		
		Label pathsDescription = new Label(pathsGroup, SWT.NONE);
		pathsDescription.setText("Each path on this list will be automatically scanned for runtimes when\n" +
				"a new workspace is created or if selected at every Eclipse startup.\n" +
				"Click Edit to configure rules/filters for the search.");
		
		Group pathsTableGroup = createGroup(composite,2);
		runtimePathViewer = createRuntimePathViewer(pathsTableGroup);
		
		Group detectorGroup = createGroup(composite,1);
		detectorGroup.setText("Available runtime detectors");
		detectorViewer = createDetectorViewer(detectorGroup);
		
//		Group runtimeDescriptionGroup = createGroup(composite,1);
//		Label runtimeDescription = new Label(runtimeDescriptionGroup, SWT.NONE);
//		runtimeDescription.setText("Runtimes found at the selected path.\n" +
//				"Remove the check mark for any runtimes you do not want identified.");
//		Group runtimeGroup = createGroup(composite,1);
//		final TableViewer runtimesViewer = createRuntimesViewer(runtimeGroup);
//		runtimePathViewer.addSelectionChangedListener(new ISelectionChangedListener() {
//			
//			@Override
//			public void selectionChanged(SelectionChangedEvent event) {
//				runtimesViewer.setInput(getServerDefinitions());
//			}
//		});
		
		Dialog.applyDialogFont(composite);
		return composite;
	}

	private TableViewer createRuntimesViewer(Composite parent) {
		CheckboxTableViewer tableViewer = CheckboxTableViewer.newCheckList(parent, SWT.V_SCROLL
				| SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		Table table = tableViewer.getTable();
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		table.setLayoutData(gd);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		String[] columnNames = new String[] { "Name", "Version", "Type", "Location", "Description"};
		int[] columnWidths = new int[] { 120, 50, 50, 150, 150};
		
		for (int i = 0; i < columnNames.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.LEFT);
			tc.setText(columnNames[i]);
			tc.setWidth(columnWidths[i]);
		}

		tableViewer.setLabelProvider(new RuntimeLabelProvider());
		final List<ServerDefinition> serverDefinitions = getServerDefinitions();
		tableViewer.setContentProvider(new RuntimeContentProvider(serverDefinitions));
		tableViewer.setInput(serverDefinitions);
		for (ServerDefinition definition:serverDefinitions) {
			tableViewer.setChecked(definition, definition.isEnabled());
		}
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			
			public void checkStateChanged(CheckStateChangedEvent event) {
				ServerDefinition definition = (ServerDefinition) event.getElement();
				definition.setEnabled(!definition.isEnabled());
//				boolean enableOk = false;
//				for (ServerDefinition serverDefinition:serverDefinitions) {
//					if (serverDefinition.isEnabled()) {
//						enableOk = true;
//					}
//				}
			}
		});
		return tableViewer;
	}

	private List<ServerDefinition> getServerDefinitions() {
		List<ServerDefinition> serverDefinitions = new ArrayList<ServerDefinition>();
		if (runtimePathViewer == null) {
			return serverDefinitions;
		}
		ISelection selection = runtimePathViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			Object object = ((IStructuredSelection)selection).getFirstElement();
			if (object instanceof RuntimePath) {
				serverDefinitions = ((RuntimePath)object).getServerDefinitions();
			}
		}
		return serverDefinitions;
	}

	private Group createGroup(Composite composite, int column) {
		GridLayout layout;
		Group group = new Group(composite, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		group.setLayoutData(gd);
		layout = new GridLayout(column, false);
		group.setLayout(layout);
		return group;
	}
	
	private TableViewer createDetectorViewer(Composite parent) {
		final CheckboxTableViewer tableViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER
				| SWT.V_SCROLL | SWT.SINGLE);
		Table table = tableViewer.getTable();
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		table.setLayoutData(gd);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		String[] columnNames = new String[] { "Type", "Link"};
		int[] columnWidths = new int[] { 300, 50};
		
		for (int i = 0; i < columnNames.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.LEFT);
			tc.setText(columnNames[i]);
			tc.setWidth(columnWidths[i]);
		}

		tableViewer.setLabelProvider(new RuntimeDetectorLabelProvider());
		tableViewer.setContentProvider(new RuntimeDetectorContentProvider(runtimeDetectors));
		
		tableViewer.setInput(runtimeDetectors);
		for (IRuntimeDetector detector:runtimeDetectors) {
			tableViewer.setChecked(detector, detector.isEnabled());
		}
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			
			public void checkStateChanged(CheckStateChangedEvent event) {
				IRuntimeDetector detector = (IRuntimeDetector) event.getElement();
				if (detector.isValid()) {
					detector.setEnabled(!detector.isEnabled());
				} else {
					MessageDialog.openWarning(getShell(), "Information", "The '" + detector.getName() + "' detector is invalid.");
					tableViewer.setChecked(detector, false);
				}
				
			}
		});
		for (int i=0; i<runtimeDetectors.size(); i++) {
			TableItem item = table.getItem(i);
			Object data = item.getData();
			if (data instanceof IRuntimeDetector) {
				IRuntimeDetector detector = (IRuntimeDetector) data;
				final String preferenceId = detector.getPreferenceId();
				if (preferenceId != null && preferenceId.trim().length() > 0) {
					Link link = new Link(table, SWT.NONE);
					link.setText("     <a>Link</a>");
					link.setEnabled(detector.isValid());
					TableEditor editor = new TableEditor (table);
					editor.grabHorizontal = editor.grabVertical = true;
					editor.setEditor (link, item, 1);
					link.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							PreferencesUtil.createPreferenceDialogOn(getShell(),preferenceId, null, null);
						}
					});
				}
			}
		}
		return tableViewer;
	}

	private TableViewer createRuntimePathViewer(Composite parent) {
		final TableViewer viewer = new TableViewer(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		viewer.getTable().setLayoutData(gd);
		
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(parent.getFont());
		
		viewer.setContentProvider(new RuntimePathContentProvider());
		
		String[] columnHeaders = {"Path", "Every start"};
		
		for (int i = 0; i < columnHeaders.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.setLabelProvider(new RuntimePathLabelProvider(i));
			column.getColumn().setText(columnHeaders[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			column.setEditingSupport(new RuntimePathEditingSupport(viewer, i));
		
		}
		
		ColumnLayoutData[] runtimePathsLayouts= {
				new ColumnWeightData(150,150),
				new ColumnWeightData(60,60)
			};
		
		TableLayout layout = new AutoResizeTableLayout(table);
		for (int i = 0; i < runtimePathsLayouts.length; i++) {
			layout.addColumnData(runtimePathsLayouts[i]);
		}
		
		viewer.getTable().setLayout(layout);
		
		configureViewer(viewer);

		
		viewer.setInput(runtimePaths);

		createRuntimePathsButtons(parent, viewer);
		return viewer;
	}

	private void configureViewer(final TableViewer viewer) {
		TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(viewer, new FocusCellOwnerDrawHighlighter(viewer));
		
		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
			protected boolean isEditorActivationEvent(
					ColumnViewerEditorActivationEvent event) {
				ViewerCell cell = viewer.getColumnViewerEditor().getFocusCell();
				if (cell != null && cell.getColumnIndex() == 1) {
					return super.isEditorActivationEvent(event);
				}
				return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
						|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		
		TableViewerEditor.create(viewer, focusCellManager, actSupport, ColumnViewerEditor.TABBING_HORIZONTAL
				| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
				| ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.KEYBOARD_ACTIVATION);
	}

	private void createRuntimePathsButtons(Composite parent, final TableViewer viewer) {
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(1,false));
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		addButton.setText("Add");
		addButton.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
				
			}

			public void widgetSelected(SelectionEvent e) {
				IDialogSettings dialogSettings = RuntimeUIActivator.getDefault().getDialogSettings();
				String lastUsedPath= dialogSettings.get(RuntimeUIActivator.LASTPATH);
				if (lastUsedPath == null) {
					lastUsedPath= ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
				}
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage("Add a new path");
				dialog.setFilterPath(lastUsedPath);
				final String path = dialog.open();
				if (path == null) {
					return;
				}
				dialogSettings.put(RuntimeUIActivator.LASTPATH, path);
				RuntimePath runtimePath = new RuntimePath(path);
				runtimePaths.add(runtimePath);
				List<RuntimePath> runtimePaths2 = new ArrayList<RuntimePath>();
				runtimePaths2.add(runtimePath);
				RuntimeUIActivator.refreshRuntimes(getShell(), runtimePaths2, null, true, 15);
				configureSearch();
				runtimePathViewer.setInput(runtimePath.getServerDefinitions());
				viewer.refresh();
			}
		
		});
		
		final Button editButton = new Button(buttonComposite, SWT.PUSH);
		editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		editButton.setText("Edit...");
		editButton.setEnabled(false);
		
		editButton.addSelectionListener(new SelectionListener(){
		
			public void widgetSelected(SelectionEvent e) {
				ISelection sel = viewer.getSelection();
				if (sel instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) sel;
					Object object = selection.getFirstElement();
					if (object instanceof RuntimePath) {
						runtimePath = (RuntimePath) object;
						RuntimePath runtimePathClone;
						try {
							runtimePathClone = (RuntimePath) runtimePath.clone();
						} catch (CloneNotSupportedException e1) {
							RuntimeUIActivator.log(e1);
							runtimePathClone = runtimePath;
						}
						EditRuntimePathDialog dialog = new EditRuntimePathDialog(getShell(), runtimePathClone);
						int ok = dialog.open();
						if (ok == Window.OK) {
							runtimePaths.remove(runtimePath);
							runtimePath = runtimePathClone;
							runtimePaths.add(runtimePath);
							configureSearch();
							viewer.refresh();
						}
					}
				}
				RuntimeUIActivator.refreshPreferencePage(getShell());
			}
		
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		final Button removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removeButton.setText("Remove");
		removeButton.setEnabled(false);
		
		removeButton.addSelectionListener(new SelectionListener(){
		
			public void widgetSelected(SelectionEvent e) {
				ISelection sel = viewer.getSelection();
				if (sel instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) sel;
					Object object = selection.getFirstElement();
					if (object instanceof RuntimePath) {
						runtimePaths.remove(object); 
						configureSearch();
						viewer.refresh();
					}
				}
			}
		
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		searchButton = new Button(buttonComposite, SWT.PUSH);
		searchButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		searchButton.setText("Search...");
		searchButton.setEnabled(runtimePaths.size() > 0);
		
		searchButton.addSelectionListener(new SelectionListener(){
		
			public void widgetSelected(SelectionEvent e) {
				RuntimeUIActivator.refreshRuntimes(getShell(), runtimePaths, null, true, 15);
			}
		
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection sel = viewer.getSelection();
				if (sel instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) sel;
					Object object = selection.getFirstElement();
					editButton.setEnabled(object instanceof RuntimePath);
					removeButton.setEnabled(object instanceof RuntimePath);
				} else {
					editButton.setEnabled(false);
					removeButton.setEnabled(false);
					//searchButton.setEnabled(false);
				}
			}
		});	
	}

	public void init(IWorkbench workbench) {
		runtimePaths = RuntimeUIActivator.getDefault().getRuntimePaths();
		runtimeDetectors = RuntimeUIActivator.getDefault().getRuntimeDetectors();
		checkboxOn = RuntimeUIActivator.imageDescriptorFromPlugin(RuntimeUIActivator.PLUGIN_ID, "/icons/xpl/complete_tsk.gif").createImage();
		checkboxOff = RuntimeUIActivator.imageDescriptorFromPlugin(RuntimeUIActivator.PLUGIN_ID, "/icons/xpl/incomplete_tsk.gif").createImage();
		errorIcon = RuntimeUIActivator.imageDescriptorFromPlugin(RuntimeUIActivator.PLUGIN_ID, "/icons/xpl/error_tsk.gif").createImage();
	}
	
	@Override
	public void dispose() {
		if (checkboxOff != null) {
			checkboxOff.dispose();
		}
		if (checkboxOn != null) {
			checkboxOn.dispose();
		}
		if (errorIcon != null) {
			errorIcon.dispose();
		}
		super.dispose();
	}
	
	/**
	 * Returns a width hint for a button control.
	 * @param button the button
	 * @return the width hint
	 */
	public static int getButtonWidthHint(Button button) {
		button.setFont(JFaceResources.getDialogFont());
		PixelConverter converter= new PixelConverter(button);
		int widthHint= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}

	/**
	 * Sets width and height hint for the button control.
	 * <b>Note:</b> This is a NOP if the button's layout data is not
	 * an instance of <code>GridData</code>.
	 *
	 * @param button	the button for which to set the dimension hint
	 */
	public static void setButtonDimensionHint(Button button) {
		Assert.isNotNull(button);
		Object gd= button.getLayoutData();
		if (gd instanceof GridData) {
			((GridData)gd).widthHint= getButtonWidthHint(button);
			((GridData)gd).horizontalAlignment = GridData.FILL;
		}
	}
	
	class RuntimePathContentProvider implements IStructuredContentProvider {

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return runtimePaths.toArray();
		}
		
		@Override
		public void dispose() {
			
		}

	}
	
	class RuntimePathLabelProvider extends ColumnLabelProvider {

		private int columnIndex;

		public RuntimePathLabelProvider(int i) {
			this.columnIndex = i;
		}

		public String getText(Object element) {
			if (element instanceof RuntimePath) {
				RuntimePath runtimePath = (RuntimePath) element;
				switch (columnIndex) {
				case 0:
					return runtimePath.getPath();
				}
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (element == null) {
				return null;
			}
			RuntimePath runtimePath = (RuntimePath) element;
			if (columnIndex == 1) {
				return runtimePath.isScanOnEveryStartup() ? checkboxOn : checkboxOff;
			}
			if (columnIndex == 0) {
				String path = runtimePath.getPath();
				if (path == null || ! (new File(path).isDirectory())) {
					return errorIcon;
				}
			}
			return null;
		}
	}
	
	private class RuntimeDetectorContentProvider implements IStructuredContentProvider {

		private Set<IRuntimeDetector> detectors;

		public RuntimeDetectorContentProvider(Set<IRuntimeDetector> detectors) {
			this.detectors = detectors;
		}
		
		public Object[] getElements(Object inputElement) {
			return detectors.toArray();
		}

		public void dispose() {
			
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			detectors = (Set<IRuntimeDetector>) newInput;
		}
	}
	
	private class RuntimeDetectorLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof IRuntimeDetector) {
				IRuntimeDetector detector = (IRuntimeDetector) element;
				if (columnIndex == 0) {
					return detector.getName();
				}
			}
			return null;
		}
	}

	@Override
	protected void performApply() {
		RuntimeUIActivator.getDefault().saveRuntimePreferences();
	}

	@Override
	protected void performDefaults() {
		RuntimeUIActivator.getDefault().initDefaultRuntimePreferences();
		runtimePaths = RuntimeUIActivator.getDefault().getRuntimePaths();
		runtimeDetectors = RuntimeUIActivator.getDefault().getRuntimeDetectors();
		runtimePathViewer.setInput(runtimePaths);
		detectorViewer.setInput(runtimeDetectors);
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		RuntimeUIActivator.getDefault().saveRuntimePreferences();
		return super.performOk();
	}

	private void configureSearch() {
		if (searchButton != null) {
			searchButton.setEnabled(runtimePaths.size() > 0);
		}
	}

}
