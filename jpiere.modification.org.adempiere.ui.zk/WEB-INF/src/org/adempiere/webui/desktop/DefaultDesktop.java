/******************************************************************************
 * Product: Posterita Ajax UI 												  *
 * Copyright (C) 2007 Posterita Ltd.  All Rights Reserved.                    *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Posterita Ltd., 3, Draper Avenue, Quatre Bornes, Mauritius                 *
 * or via info@posterita.org or http://www.posterita.org/                     *
 *****************************************************************************/

package org.adempiere.webui.desktop;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.base.Service;
import org.adempiere.base.event.EventManager;
import org.adempiere.base.event.IEventManager;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.model.MBroadcastMessage;
import org.adempiere.util.ServerContext;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.adwindow.ADWindow;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.apps.BusyDialog;
import org.adempiere.webui.apps.DesktopRunnable;
import org.adempiere.webui.apps.ProcessDialog;
import org.adempiere.webui.apps.WReport;
import org.adempiere.webui.component.DesktopTabpanel;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.ToolBar;
import org.adempiere.webui.component.ToolBarButton;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.event.DrillEvent;
import org.adempiere.webui.event.MenuListener;
import org.adempiere.webui.event.ZKBroadCastManager;
import org.adempiere.webui.event.ZoomEvent;
import org.adempiere.webui.factory.IFormWindowZoomFactory;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.BroadcastMessageWindow;
import org.adempiere.webui.panel.HeaderPanel;
import org.adempiere.webui.panel.HelpController;
import org.adempiere.webui.panel.TimeoutPanel;
import org.adempiere.webui.session.SessionContextListener;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.IServerPushCallback;
import org.adempiere.webui.util.ServerPushTemplate;
import org.adempiere.webui.util.UserPreference;
import org.adempiere.webui.window.FDialog;
import org.compiere.Adempiere;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.I_AD_Preference;
import org.compiere.model.MPreference;
import org.compiere.model.MQuery;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.model.X_AD_CtxHelp;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.util.WebUtil;
import org.idempiere.broadcast.BroadCastMsg;
import org.idempiere.broadcast.BroadCastUtil;
import org.idempiere.broadcast.BroadcastMsgUtil;
import org.osgi.service.event.EventHandler;
import org.zkoss.zk.au.out.AuScript;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.event.SwipeEvent;
import org.zkoss.zk.ui.metainfo.PageDefinition;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.DesktopCleanup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.East;
import org.zkoss.zul.West;

/**
 *
 * Default desktop implementation.
 * @author <a href="mailto:agramdass@gmail.com">Ashley G Ramdass</a>
 * @author <a href="mailto:hengsin@gmail.com">Low Heng Sin</a>
 * @date Mar 2, 2007
 * @version $Revision: 0.10 $
 * @author Deepak Pansheriya/Vivek - Adding support for message broadcasting
 */
public class DefaultDesktop extends TabbedDesktop implements MenuListener, Serializable, EventListener<Event>, EventHandler, DesktopCleanup
{
	/**
	 *
	 */
	private static final long serialVersionUID = 6775071898539380777L;

	private static final String IMAGES_UPARROW_PNG = "images/collapse-header.png";

	private static final String IMAGES_DOWNARROW_PNG = "images/expand-header.png";

	private static final String IMAGES_CONTEXT_HELP_PNG = "images/Help16.png";

	private static final String IMAGES_THREELINE_MENU_PNG = "images/threelines.png";

	@SuppressWarnings("unused")
	private static final CLogger logger = CLogger.getCLogger(DefaultDesktop.class);

	private Borderlayout layout;

	private int noOfNotice;

	private int noOfRequest;

	private int noOfWorkflow;

	private int noOfUnprocessed;

	private Tabpanel homeTab;

	private DashboardController dashboardController, sideController;

	private HeaderPanel pnlHead;

	private Desktop m_desktop = null;

	private HelpController helpController;

	private ToolBarButton max;

	private ToolBarButton contextHelp;

	private ToolBarButton showHeader;

	private Component headerContainer;

	private Window headerPopup;

    public DefaultDesktop()
    {
    	super();
    	dashboardController = new DashboardController();
    	sideController = new DashboardController();
    	helpController = new HelpController();

    	m_desktop = AEnv.getDesktop();
    	m_desktop.addListener(this);
    	//subscribing to broadcast event
    	bindEventManager();
    	ZKBroadCastManager.getBroadCastMgr();

    	EventQueue<Event> queue = EventQueues.lookup(ACTIVITIES_EVENT_QUEUE, true);
    	queue.subscribe(this);

    	//JPIERE-0139:Zoom to Form Window - start
    	if(MTable.getTable_ID("JP_FormWindowZoom")== 0)
    		return ;

		String sql = "SELECT AD_Window_ID, JP_ZoomWindow_ID FROM JP_FormWindowZoom WHERE IsActive='Y'";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			while (rs.next())
				fromWindowToFormMap.put(rs.getInt(1), rs.getInt(2));
		}
		catch (Exception e)
		{
//				log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		//JPiere-0139:Zoom to Form Window - finish

    }

    @SuppressWarnings("serial")
	protected Component doCreatePart(Component parent)
    {
    	PageDefinition pagedef = Executions.getCurrent().getPageDefinition(ThemeManager.getThemeResource("zul/desktop/desktop.zul"));
    	Component page = Executions.createComponents(pagedef, parent, null);
    	layout = (Borderlayout) page.getFellow("layout");
    	headerContainer = page.getFellow("northBody");
    	pnlHead = (HeaderPanel) headerContainer.getFellow("header");

        layout.addEventListener("onZoom", this);
        layout.addEventListener(DrillEvent.ON_DRILL_DOWN, this);

        West w = layout.getWest();
        w.addEventListener(Events.ON_OPEN, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				OpenEvent oe = (OpenEvent) event;
				updateMenuCollapsedPreference(!oe.isOpen());
			}
		});
        w.addEventListener(Events.ON_SWIPE, new EventListener<SwipeEvent>() {

			@Override
			public void onEvent(SwipeEvent event) throws Exception {
				if ("left".equals(event.getSwipeDirection())) {
					West w = (West) event.getTarget();
					if (w.isOpen()) {
						w.setOpen(false);
						updateMenuCollapsedPreference(true);
					}
				}
			}
		});

        w.addEventListener(Events.ON_SIZE, new EventListener<Event>() {

        	@Override
        	public void onEvent(Event event) throws Exception {
        			West west = (West) event.getTarget();
        			updateSideControllerWidthPreference(west.getWidth());
        	}
        });

        UserPreference pref = SessionManager.getSessionApplication().getUserPreference();
        boolean menuCollapsed= pref.isPropertyBool(UserPreference.P_MENU_COLLAPSED);
        w.setOpen(!menuCollapsed);

        East e = layout.getEast();
        e.addEventListener(Events.ON_OPEN, new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				OpenEvent oe = (OpenEvent) event;
				updateHelpCollapsedPreference(!oe.isOpen());
				HtmlBasedComponent comp = windowContainer.getComponent();
				if (comp != null) {
					contextHelp.setVisible(!oe.isOpen());
					if (!oe.isOpen())
						layout.getEast().setVisible(false);
				}
			}
		});

        e.addEventListener(Events.ON_SWIPE, new EventListener<SwipeEvent>() {

			@Override
			public void onEvent(SwipeEvent event) throws Exception {
				if ("right".equals(event.getSwipeDirection())) {
					East e = (East) event.getTarget();
					if (e.isOpen()) {
						e.setOpen(false);
						updateHelpCollapsedPreference(true);
					}
				}
			}
		});

        e.addEventListener(Events.ON_SIZE, new EventListener<Event>() {

        	@Override
        	public void onEvent(Event event) throws Exception {
        			East east = (East) event.getTarget();
        			updateHelpWidthPreference(east.getWidth());
        	}
        });

        String westWidth = getWestWidthPreference();
        String eastWidth = getEastWidthPreference();

        //Set preference width
        if( westWidth != null || eastWidth != null ){

        	//If both panels have prefered size check that the sum is not bigger than the browser
        	if( westWidth != null && eastWidth != null ){
            	ClientInfo browserInfo = getClientInfo();
        		int browserWidth = browserInfo.desktopWidth;
        		int wWidth = Integer.valueOf(westWidth.replace("px", ""));
        		int eWidth = Integer.valueOf(eastWidth.replace("px", ""));

        		if( eWidth + wWidth <= browserWidth ){
        			w.setWidth(westWidth);
        			e.setWidth(eastWidth);
        		}

        	}
        	else if ( westWidth != null )
            	w.setWidth(westWidth);

        	else if ( eastWidth != null )
            	e.setWidth(eastWidth);
        }

        boolean helpCollapsed= pref.isPropertyBool(UserPreference.P_HELP_COLLAPSED);
        e.setVisible(!helpCollapsed);

        helpController.render(e, this);

        Center windowArea = layout.getCenter();

        windowContainer.createPart(windowArea);

        homeTab = new Tabpanel();
        windowContainer.addWindow(homeTab, Util.cleanAmp(Msg.getMsg(Env.getCtx(), "Home")), false, null);
        homeTab.getLinkedTab().setSclass("desktop-hometab");
        homeTab.setSclass("desktop-home-tabpanel");
        BusyDialog busyDialog = new BusyDialog();
        busyDialog.setShadow(false);
        homeTab.appendChild(busyDialog);

        // register as 0
        registerWindow(homeTab);

        BroadcastMessageWindow messageWindow = new BroadcastMessageWindow(pnlHead);
        BroadcastMsgUtil.showPendingMessage(Env.getAD_User_ID(Env.getCtx()), messageWindow);

        if (!layout.getDesktop().isServerPushEnabled())
    	{
    		layout.getDesktop().enableServerPush(true);
    	}

        Runnable runnable = new Runnable() {
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}

				IServerPushCallback callback = new IServerPushCallback() {
					public void updateUI() {
						Properties ctx = (Properties)layout.getDesktop().getSession().getAttribute(SessionContextListener.SESSION_CTX);
						try {
							ServerContext.setCurrentInstance(ctx);
							renderHomeTab();
						} finally {
							ServerContext.dispose();
						}
					}
				};
				ServerPushTemplate template = new ServerPushTemplate(layout.getDesktop());
				template.executeAsync(callback);
			}
		};

		Adempiere.getThreadPoolExecutor().submit(new DesktopRunnable(runnable,layout.getDesktop()));

		ToolBar toolbar = new ToolBar();
        windowContainer.getComponent().appendChild(toolbar);

        showHeader = new ToolBarButton() {
			@Override
			public void onPageDetached(Page page) {
				super.onPageDetached(page);
				if (DefaultDesktop.this.headerPopup != null) {
					DefaultDesktop.this.headerPopup.setPage(null);
				}
			}

        };
        toolbar.appendChild(showHeader);
        showHeader.setImage(ThemeManager.getThemeResource(IMAGES_THREELINE_MENU_PNG));
        showHeader.addEventListener(Events.ON_CLICK, this);
        showHeader.setSclass("window-container-toolbar-btn");
        showHeader.setStyle("cursor: pointer; border: 1px solid transparent; padding: 2px;");
        showHeader.setVisible(false);

        max = new ToolBarButton();
        toolbar.appendChild(max);
        max.setImage(ThemeManager.getThemeResource(IMAGES_UPARROW_PNG));
        max.addEventListener(Events.ON_CLICK, this);
        max.setSclass("window-container-toolbar-btn");
        max.setStyle("cursor: pointer; border: 1px solid transparent; padding: 2px;");

        contextHelp = new ToolBarButton();
        //IDEMPIERE-0120
//        toolbar.appendChild(contextHelp);
//        contextHelp.setImage(ThemeManager.getThemeResource(IMAGES_CONTEXT_HELP_PNG));
//        contextHelp.addEventListener(Events.ON_CLICK, this);
//        contextHelp.setSclass("window-container-toolbar-btn context-help-btn");
//        contextHelp.setStyle("cursor: pointer; border: 1px solid transparent; padding: 2px;");
//        contextHelp.setTooltiptext(Util.cleanAmp(Msg.getElement(Env.getCtx(), "AD_CtxHelp_ID")));
        contextHelp.setVisible(false);

        boolean headerCollapsed= pref.isPropertyBool(UserPreference.P_HEADER_COLLAPSED);
        if (headerCollapsed) {
        	collapseHeader();
        }

        return layout;
    }

    private String getWestWidthPreference() {

    	String width = Env.getPreference(Env.getCtx(), 0, "SideController.Width", false);

    	if( (! Util.isEmpty(width)) ){
        	ClientInfo browserInfo = getClientInfo();
    		int browserWidth = browserInfo.desktopWidth;
    		int prefWidth = Integer.valueOf(width.replace("px", ""));

    		if( prefWidth <= browserWidth )
    			return width;
    	}

		return null;
	}

	protected void updateSideControllerWidthPreference(String width) {

    	if( width != null ){
        	Query query = new Query(Env.getCtx(),
        			MTable.get(Env.getCtx(), I_AD_Preference.Table_ID),
        			" Attribute=? AND AD_User_ID=? AND AD_Process_ID IS NULL AND PreferenceFor = 'W'",
        			null);

        	int userId = Env.getAD_User_ID(Env.getCtx());
        	MPreference preference = query.setOnlyActiveRecords(true)
        			.setApplyAccessFilter(true)
        			.setParameters("SideController.Width", userId)
        			.first();

        	if ( preference == null || preference.getAD_Preference_ID() <= 0 ) {

        		preference = new MPreference(Env.getCtx(), 0, null);
        		preference.set_ValueOfColumn("AD_User_ID", userId); // required set_Value for System=0 user
        		preference.setAttribute("SideController.Width");
        	}
        	preference.setValue(width);
        	preference.saveEx();

    	}

	}

	private String getEastWidthPreference() {

    	String width = Env.getPreference(Env.getCtx(), 0, "HelpController.Width", false);

    	if( (! Util.isEmpty(width)) ){
        	ClientInfo browserInfo = getClientInfo();
    		int browserWidth = browserInfo.desktopWidth;
    		int prefWidth = Integer.valueOf(width.replace("px", ""));

    		if( prefWidth <=  browserWidth )
    			return width;
    	}

		return null;
	}

	protected void updateHelpWidthPreference(String width) {

    	if( width != null ){
        	Query query = new Query(Env.getCtx(),
        			MTable.get(Env.getCtx(), I_AD_Preference.Table_ID),
        			" Attribute=? AND AD_User_ID=? AND AD_Process_ID IS NULL AND PreferenceFor = 'W'",
        			null);

        	int userId = Env.getAD_User_ID(Env.getCtx());
        	MPreference preference = query.setOnlyActiveRecords(true)
        			.setApplyAccessFilter(true)
        			.setParameters("HelpController.Width", userId)
        			.first();

        	if ( preference == null || preference.getAD_Preference_ID() <= 0 ) {

        		preference = new MPreference(Env.getCtx(), 0, null);
        		preference.set_ValueOfColumn("AD_User_ID", userId); // required set_Value for System=0 user
        		preference.setAttribute("HelpController.Width");
        	}
        	preference.setValue(width);
        	preference.saveEx();
    	}

    }

	private void updateMenuCollapsedPreference(boolean collapsed) {
		UserPreference pref = SessionManager.getSessionApplication().getUserPreference();
		pref.setProperty(UserPreference.P_MENU_COLLAPSED, collapsed);
		pref.savePreference();
	}

	private void updateHelpCollapsedPreference(boolean collapsed) {
		UserPreference pref = SessionManager.getSessionApplication().getUserPreference();
		pref.setProperty(UserPreference.P_HELP_COLLAPSED, collapsed);
		pref.savePreference();
	}

	private void updateHeaderCollapsedPreference(boolean collapsed) {
		UserPreference pref = SessionManager.getSessionApplication().getUserPreference();
		pref.setProperty(UserPreference.P_HEADER_COLLAPSED, collapsed);
		pref.savePreference();
	}

	public void renderHomeTab()
	{
		homeTab.getChildren().clear();

		dashboardController.render(homeTab, this, true);

		West w = layout.getWest();
		w.getChildren().clear();
		sideController.render(w, this, false);

	}

	public void onEvent(Event event)
    {
        Component comp = event.getTarget();
        String eventName = event.getName();

        if(eventName.equals(Events.ON_CLICK))
        {
        	if (comp == max)
        	{
        		if (layout.getNorth().isVisible())
        		{
        			collapseHeader();
        		}
        		else
        		{
        			restoreHeader();
        		}
        	}
        	else if (comp == showHeader)
        	{
    			showHeader.setPressed(true);
    			if (pnlHead.getParent() != headerPopup)
    				headerPopup.appendChild(pnlHead);
    			LayoutUtils.openPopupWindow(showHeader, headerPopup, "after_start");
        	}

        	//JPIERE-0120:
//        	else if (comp == contextHelp)
//        	{
//        		layout.getEast().setVisible(true);
//        		layout.getEast().setOpen(true);
//        		contextHelp.setVisible(false);
//        		updateHelpCollapsedPreference(false);
//        	}

        	else if(comp instanceof ToolBarButton)
            {
            	ToolBarButton btn = (ToolBarButton) comp;

            	if (btn.getAttribute("AD_Menu_ID") != null)
            	{
	            	int menuId = (Integer)btn.getAttribute("AD_Menu_ID");
	            	if(menuId > 0) onMenuSelected(menuId);
            	}
            }
        }
        else if (eventName.equals(ON_ACTIVITIES_CHANGED_EVENT))
        {
        	@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) event.getData();
        	Integer notice = (Integer) map.get("notice");
        	Integer request = (Integer) map.get("request");
        	Integer workflow = (Integer) map.get("workflow");
        	Integer unprocessed = (Integer) map.get("unprocessed");
        	boolean change = false;
        	if (notice != null && notice.intValue() != noOfNotice)
        	{
        		noOfNotice = notice.intValue(); change = true;
        	}
        	if (request != null && request.intValue() != noOfRequest)
        	{
        		noOfRequest = request.intValue(); change = true;
        	}
        	if (workflow != null && workflow.intValue() != noOfWorkflow)
        	{
        		noOfWorkflow = workflow.intValue(); change = true;
        	}
        	if (unprocessed != null && unprocessed.intValue() != noOfUnprocessed)
        	{
        		noOfUnprocessed = unprocessed.intValue(); change = true;
        	}
        	if (change)
        		updateUI();
        }
        else if (event instanceof ZoomEvent)
		{
        	Clients.clearBusy();
			ZoomEvent ze = (ZoomEvent) event;
			if (ze.getData() != null && ze.getData() instanceof MQuery) {
				AEnv.zoom((MQuery) ze.getData());
			}
		}

        else if (event instanceof DrillEvent)
		{
        	Clients.clearBusy();
			DrillEvent de = (DrillEvent) event;
			if (de.getData() != null && de.getData() instanceof MQuery) {
				MQuery query = (MQuery) de.getData();
				executeDrill(query);
			}
		}
    }

	protected void restoreHeader() {
		layout.getNorth().setVisible(true);
		max.setImage(ThemeManager.getThemeResource(IMAGES_UPARROW_PNG));
		showHeader.setVisible(false);
		pnlHead.detach();
		headerContainer.appendChild(pnlHead);
		Clients.resize(pnlHead);
		updateHeaderCollapsedPreference(false);
	}

	protected void collapseHeader() {
		layout.getNorth().setVisible(false);
		max.setImage(ThemeManager.getThemeResource(IMAGES_DOWNARROW_PNG));
		showHeader.setVisible(true);
		pnlHead.detach();
		if (headerPopup == null)
		{
			headerPopup = new Window();
			headerPopup.setSclass("desktop-header-popup");
			headerPopup.setVflex("true");
			headerPopup.setVisible(false);
			headerPopup.addEventListener(Events.ON_OPEN, new EventListener<OpenEvent>() {
				@Override
				public void onEvent(OpenEvent event) throws Exception {
					if (!event.isOpen()) {
						if (showHeader.isPressed())
							showHeader.setPressed(false);
					}
				}
			});
		}
		headerPopup.appendChild(pnlHead);
		updateHeaderCollapsedPreference(true);
	}

	/**
	 * 	Execute Drill to Query
	 * 	@param query query
	 */
   	private void executeDrill (MQuery query)
	{
		int AD_Table_ID = MTable.getTable_ID(query.getTableName());
		if (!MRole.getDefault().isCanReport(AD_Table_ID))
		{
			FDialog.error(0, null, "AccessCannotReport", query.getTableName());
			return;
		}
		if (AD_Table_ID != 0)
			new WReport(AD_Table_ID, query);
	}	//	executeDrill

	/**
	 *
	 * @param page
	 */
	public void setPage(Page page) {
		if (this.page != page) {
			layout.setPage(page);
			this.page = page;

			if (dashboardController != null) {
				dashboardController.onSetPage(page, layout.getDesktop());
			}

			if (sideController != null) {
				sideController.onSetPage(page, layout.getDesktop());
			}
		}
	}

	/**
	 * Get the root component
	 * @return Component
	 */
	public Component getComponent() {
		return layout;
	}

	public void logout() {
		unbindEventManager();
		if (dashboardController != null) {
			dashboardController.onLogOut();
			dashboardController = null;
		}

		if (sideController != null) {
			sideController.onLogOut();
			sideController = null;
		}
		layout.detach();
		layout = null;
		pnlHead = null;
		max = null;
		m_desktop = null;
	}

	public void updateUI() {
		int total = noOfNotice + noOfRequest + noOfWorkflow + noOfUnprocessed;
		windowContainer.setTabTitle(0, Util.cleanAmp(Msg.getMsg(Env.getCtx(), "Home"))
				+ " (" + total + ")",
				Msg.translate(Env.getCtx(), "AD_Note_ID") + " : " + noOfNotice
				+ ", " + Msg.translate(Env.getCtx(), "R_Request_ID") + " : " + noOfRequest
				+ ", " + Util.cleanAmp(Msg.getMsg (Env.getCtx(), "WorkflowActivities")) + " : " + noOfWorkflow
				+ (noOfUnprocessed>0 ? ", " + Msg.getMsg (Env.getCtx(), "UnprocessedDocs") + " : " + noOfUnprocessed : "")
				);
	}

	//use _docClick undocumented api. need verification after major zk release update
	private final static String autoHideMenuScript = "try{var w=zk.Widget.$('#{0}');var t=zk.Widget.$('#{1}');" +
			"var e=new Object;e.target=t;w._docClick(e);}catch(error){}";

	private void autoHideMenu() {
		if (layout.getWest().isCollapsible() && !layout.getWest().isOpen())
		{
			String id = layout.getWest().getUuid();
			Tab tab = windowContainer.getSelectedTab();
			if (tab != null) {
				String tabId = tab.getUuid();
				String script = autoHideMenuScript.replace("{0}", id);
				script = script.replace("{1}", tabId);
				AuScript aus = new AuScript(layout.getWest(), script);
				Clients.response("autoHideWest", aus);
			}
		}
	}

	@Override
	protected void preOpenNewTab()
	{
		autoHideMenu();
	}

	//Implementation for Broadcast message
	/**
	 * @param eventManager
	 */
	public void bindEventManager() {
		EventManager.getInstance().register(IEventTopics.BROADCAST_MESSAGE, this);
	}

	/**
	 * @param eventManager
	 */
	public void unbindEventManager() {
		EventManager.getInstance().unregister(this);
	}

	@Override
	public void handleEvent(final org.osgi.service.event.Event event) {
		String eventName = event.getTopic();
		if (eventName.equals(IEventTopics.BROADCAST_MESSAGE)) {
			EventListener<Event> listner = new EventListener<Event>() {

				@Override
				public void onEvent(Event event) throws Exception {
					BroadCastMsg msg = (BroadCastMsg) event.getData();


					switch (msg.getEventId()) {
					case BroadCastUtil.EVENT_TEST_BROADCAST_MESSAGE:
						MBroadcastMessage mbMessage = MBroadcastMessage.get(
								Env.getCtx(), msg.getIntData());
						String currSession = Integer
								.toString(Env.getContextAsInt(Env.getCtx(),
										"AD_Session_ID"));
						if (currSession.equals(msg.getTarget())) {
							BroadcastMessageWindow testMessageWindow = new BroadcastMessageWindow(
										pnlHead);
							testMessageWindow.appendMessage(mbMessage, true);
							testMessageWindow = null;

						}
						break;
					case BroadCastUtil.EVENT_BROADCAST_MESSAGE:
						mbMessage = MBroadcastMessage.get(
								Env.getCtx(), msg.getIntData());
						if (mbMessage.isValidUserforMessage()) {

							BroadcastMessageWindow messageWindow = new BroadcastMessageWindow(
										pnlHead);
							messageWindow.appendMessage(mbMessage, false);
						}
						break;
					case BroadCastUtil.EVENT_SESSION_TIMEOUT:

						currSession = Integer.toString(Env.getContextAsInt(
								Env.getCtx(), "AD_Session_ID"));
						if (currSession.equalsIgnoreCase(msg.getTarget())) {
							new TimeoutPanel(pnlHead, msg.getIntData());
						}

						break;
					case BroadCastUtil.EVENT_SESSION_ONNODE_TIMEOUT:

						currSession = WebUtil.getServerName();

						if (currSession.equalsIgnoreCase(msg.getTarget())) {
							new TimeoutPanel(pnlHead, msg.getIntData());
						}

					}

				}

			};

			Executions.schedule(m_desktop, listner, new Event("OnBroadcast",
					null, event.getProperty(IEventManager.EVENT_DATA)));

		}
	}

	@Override
	public void cleanup(Desktop desktop) throws Exception {
		unbindEventManager();
	}

	//JPIERE-0120:
	@Override
	public void updateHelpContext(String ctxType, int recordId) {
		// don't show context for SetupWizard Form, is managed internally using wf and node ctxhelp
//		if (recordId == SystemIDs.FORM_SETUP_WIZARD && X_AD_CtxHelp.CTXTYPE_Form.equals(ctxType))
//			return;

//		Clients.response(new AuScript("zWatch.fire('onFieldTooltip', this);"));
//		helpController.renderCtxHelp(ctxType, recordId);
//
//		GridTab gridTab = null;
//		Component window = getActiveWindow();
//		ADWindow adwindow = ADWindow.findADWindow(window);
//		if (adwindow != null) {
//			gridTab = adwindow.getADWindowContent().getActiveGridTab();
//		}
//		updateHelpQuickInfo(gridTab);
	}

	@Override
	public void updateHelpTooltip(GridField gridField) {
//		helpController.renderToolTip(gridField);//JPIERE-0120:
	}

	@Override
	public void updateHelpTooltip(String hdr, String  desc, String help, String otherContent) {
//		helpController.renderToolTip(hdr, desc, help, otherContent);//JPIERE-0120:
	}

	@Override
	public void updateHelpQuickInfo(GridTab gridTab) {
//		helpController.renderQuickInfo(gridTab);//JPIERE-0120:
	}

	@Override
	public ProcessDialog openProcessDialog(int processId, boolean soTrx) {
		ProcessDialog pd = super.openProcessDialog(processId, soTrx);
		updateHelpContext(X_AD_CtxHelp.CTXTYPE_Process, processId);
		return pd;
	}

	@Override
	public ADForm openForm(int formId) {
		ADForm form = super.openForm(formId);
		updateHelpContext(X_AD_CtxHelp.CTXTYPE_Form, formId);
		return form;
	}

	@Override
	public void openInfo(int infoId) {
		super.openInfo(infoId);
		updateHelpContext(X_AD_CtxHelp.CTXTYPE_Info, infoId);
	}

	@Override
	public void openWorkflow(int workflow_ID) {
		super.openWorkflow(workflow_ID);
		updateHelpContext(X_AD_CtxHelp.CTXTYPE_Workflow, workflow_ID);
	}

	@Override
	public void openTask(int taskId) {
		super.openTask(taskId);
		updateHelpContext(X_AD_CtxHelp.CTXTYPE_Task, taskId);
	}

    public boolean isPendingWindow() {
        List<Object> windows = getWindows();
        if (windows != null) {
        	for (int idx = 0; idx < windows.size(); idx++) {
                Object ad = windows.get(idx);
                if (ad != null && ad instanceof ADWindow && ((ADWindow)ad).getADWindowContent() != null) {
                	if ( ((ADWindow)ad).getADWindowContent().isPendingChanges()) {
                		return true;
                	}
                }
        	}
        }
        return false;
    }

	@Override
	public void onMenuSelected(int menuId) {
		super.onMenuSelected(menuId);
		if (showHeader.isVisible()) {
			//ensure header popup is close
			String script = "var w=zk.Widget.$('#" + layout.getUuid()+"'); " +
					"zWatch.fire('onFloatUp', w);";
			Clients.response(new AuScript(script));
		}
	}

	//JPIERE-0139:Zoom to Form Window
	HashMap<Integer, Integer> fromWindowToFormMap = new HashMap<Integer, Integer>();
	HashMap<Integer, String> WindowTabTitleMap = new HashMap<Integer, String>();

	/**
	 * JPIERE-0139 Zoom to Form Window
	 *
	 */
	@Override
	public void showZoomWindow(int AD_Window_ID, MQuery query) {

		if(fromWindowToFormMap.size() > 0 && fromWindowToFormMap.containsKey(AD_Window_ID))
		{
			int Zoom_Window_ID = ((Integer)fromWindowToFormMap.get(AD_Window_ID)).intValue();

			//Zoom to Form Window
			List<IFormWindowZoomFactory> factories = Service.locator().list(IFormWindowZoomFactory.class).getServices();
			if (factories != null)
			{
				for(IFormWindowZoomFactory factory : factories)
				{
					ADForm form = factory.newFormInstance(Zoom_Window_ID, query);
					if(form != null)
					{
						if (Window.Mode.EMBEDDED == form.getWindowMode()) {
							DesktopTabpanel tabPanel = new DesktopTabpanel();
							form.setParent(tabPanel);
							//do not show window title when open as tab
							form.setTitle(null);
							preOpenNewTab();
							if(!WindowTabTitleMap.containsKey(AD_Window_ID))
								WindowTabTitleMap.put(AD_Window_ID, MWindow.get(Env.getCtx(), AD_Window_ID).get_Translation("Name"));
							windowContainer.addWindow(tabPanel, WindowTabTitleMap.get(AD_Window_ID), true, null);
							form.focus();
						} else {
//							form.setAttribute(Window.MODE_KEY, form.getWindowMode());
//							showWindow(form);
						}
					}

				}//for

			}//if (factories != null)

			return;
		}

		super.showZoomWindow(AD_Window_ID, query);

	}//showZoomWindow

}