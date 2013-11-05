package com.eviware.soapui.impl.rest.panels.request;

import com.eviware.soapui.SoapUI;
import org.fest.swing.core.BasicRobot;
import org.fest.swing.core.ComponentDragAndDrop;
import org.fest.swing.core.Robot;
import org.fest.swing.core.Settings;
import org.fest.swing.fixture.*;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

import static com.eviware.soapui.impl.rest.actions.support.NewRestResourceActionBase.ParamLocation;
import static com.eviware.soapui.impl.rest.panels.method.RestMethodDesktopPanel.REST_METHOD_EDITOR;
import static com.eviware.soapui.impl.rest.panels.request.RestRequestDesktopPanel.REST_REQUEST_EDITOR;
import static com.eviware.soapui.impl.rest.panels.resource.RestParamsTable.REST_PARAMS_TABLE;
import static com.eviware.soapui.impl.rest.panels.resource.RestResourceDesktopPanel.REST_RESOURCE_EDITOR;
import static com.eviware.soapui.impl.wsdl.panels.teststeps.support.AddParamAction.ADD_PARAM_ACTION_NAME;
import static com.eviware.soapui.ui.Navigator.NAVIGATOR;
import static com.eviware.soapui.utils.FestMatchers.*;
import static org.fest.swing.data.TableCell.row;
import static org.fest.swing.launcher.ApplicationLauncher.application;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Prakash
 * Date: 2013-10-29
 * Time: 14:37
 * To change this template use File | Settings | File Templates.
 */
public class SynchParametersTest
{
	private static final int REST_RESOURCE_POSITION_IN_TREE = 3;
	private static final int REST_REQUEST_POSITION_IN_TREE = 5;
	private static final int REST_METHOD_POSITION_IN_TREE = 4;
	private Robot robot;

	@Before
	public void setUp()
	{
		application( SoapUI.class ).start();
		robot = BasicRobot.robotWithCurrentAwtHierarchy();
		robot.settings().delayBetweenEvents( 200 );
	}

	@Test
	public void test() throws InterruptedException
	{
		FrameFixture rootWindow = frameWithTitle( "SoapUI" ).using( robot );
		rootWindow.maximize();

		createNewRestProject( rootWindow );

		JPanelFixture resourceEditor = openResourceEditor( rootWindow );

		JPanelFixture requestEditor = openRequestEditor( rootWindow );

		addNewParameter( requestEditor, "ParamName", "value" );
		verifyParamValues( requestEditor, 0, "ParamName", "value" );
		verifyParamValues( resourceEditor, 0, "ParamName", "" );

		openResourceEditor( rootWindow );

		addNewParameter( resourceEditor, "resParam", "value1" );
		verifyParamValues( resourceEditor, 1, "resParam", "value1" );
		verifyParamValues( requestEditor, 1, "resParam", "value1" );

		JPanelFixture methodEditor = openMethodEditor( rootWindow );
		addNewParameter( methodEditor, "methodParam", "mValue" );
		verifyParamValues( methodEditor, 0, "methodParam", "mValue" );
		verifyParamValues( requestEditor, 2, "methodParam", "mValue" );


		changeParameterLevel( methodEditor, 0, ParamLocation.RESOURCE );
		verifyParamValues( resourceEditor, 2, "methodParam", "mValue" );

		verifyEmptyTable( methodEditor );

		openResourceEditor( rootWindow );

		closeWindow( rootWindow );

	}

	private void createNewRestProject( FrameFixture rootWindow )
	{
		openCreateNewRestProjectDialog( rootWindow );

		enterURIandClickOk();
	}

	private void closeWindow( FrameFixture rootWindow ) throws InterruptedException
	{
		Thread.sleep( 2000 );
		rootWindow.close();

		DialogFixture confirmationDialog = dialogWithTitle( "Question" ).using( robot );
		confirmationDialog.button( buttonWithText( "Yes" ) ).click();

		DialogFixture saveProjectDialog = dialogWithTitle( "Save Project" ).using( robot );
		while( saveProjectDialog != null )
		{
			saveProjectDialog.button( buttonWithText( "No" ) ).click();
			saveProjectDialog = dialogWithTitle( "Save Project" ).using( robot );
		}
	}

	private void verifyParamValues( JPanelFixture parentPanel, int rowNum, String paramName, String paramValue )
	{
		JTableFixture paramTableInResourceEditor = parentPanel.table( REST_PARAMS_TABLE );
		assertThat( paramTableInResourceEditor.cell( row( rowNum ).column( 0 ) ).value(), is( paramName ) );
		assertThat( paramTableInResourceEditor.cell( row( rowNum ).column( 1 ) ).value(), is( paramValue ) );
	}

	private void openCreateNewRestProjectDialog( FrameFixture rootWindow )
	{
		JPopupMenuFixture projects = rightClickOnProjectsMenu( rootWindow );

		JMenuItemFixture createNewRestProjectMenu = projects.menuItem( menuItemWithText( "New REST Project" ) );
		createNewRestProjectMenu.click();
	}

	private void addNewParameter( JPanelFixture parentPanel, String paramName, String paramValue )
	{
		parentPanel.button( ADD_PARAM_ACTION_NAME ).click();
		JTableFixture restParamsTable = parentPanel.table( REST_PARAMS_TABLE );

		robot.waitForIdle();
		int rowNumToEdit = restParamsTable.target.getRowCount() - 1;
		JTableCellFixture cellFixture = restParamsTable.cell( row( rowNumToEdit ).column( 0 ) );
		JTextComponentFixture textBox = new JTextComponentFixture( robot, ( JTextField )cellFixture.editor() );
		textBox.enterText( paramName );
		textBox.pressKey( KeyEvent.VK_ENTER );

		robot.waitForIdle();
		textBox = new JTextComponentFixture( robot, ( JTextField )restParamsTable.cell( row( rowNumToEdit ).column( 1 ) ).editor() );
		textBox.enterText( paramValue );
		textBox.pressKey( KeyEvent.VK_ENTER );
	}

	private void changeParameterLevel( JPanelFixture parentPanel, int rownum, ParamLocation newLocation )
	{
		JTableFixture restParamsTable = parentPanel.table( REST_PARAMS_TABLE );
		restParamsTable.cell( row( rownum ).column( 3 ) ).enterValue( newLocation.toString() );
	}

	public void verifyEmptyTable( JPanelFixture parentPanel )
	{
		JTableFixture restParamsTable = parentPanel.table( REST_PARAMS_TABLE );
		assertThat( restParamsTable.target.getRowCount(), is( 0 ) );
	}

	private JPanelFixture openMethodEditor( FrameFixture frame )
	{
		return getPanelFixture( frame, REST_METHOD_POSITION_IN_TREE, REST_METHOD_EDITOR );
	}

	private JPanelFixture openResourceEditor( FrameFixture frame )
	{
		return getPanelFixture( frame, REST_RESOURCE_POSITION_IN_TREE, REST_RESOURCE_EDITOR );
	}

	private JPanelFixture openRequestEditor( FrameFixture frame )
	{
		return getPanelFixture( frame, REST_REQUEST_POSITION_IN_TREE, REST_REQUEST_EDITOR );
	}

	private JPanelFixture getPanelFixture( FrameFixture frame, int panelPositionInNavigationTree, String panelName )
	{
		getNavigatorPanel( frame ).tree().node( panelPositionInNavigationTree ).doubleClick();
		return frame.panel( panelName );
	}

	private JPopupMenuFixture rightClickOnProjectsMenu( FrameFixture frame )
	{
		return getNavigatorPanel( frame ).tree().showPopupMenuAt( "Projects" );
	}

	private JPanelFixture getNavigatorPanel( FrameFixture frame )
	{
		return frame.panel( NAVIGATOR );
	}

	private void enterURIandClickOk()
	{
		DialogFixture newRestProjectDialog = dialogWithTitle( "New REST Project" ).withTimeout( 2000 )
				.using( robot );

		newRestProjectDialog.textBox().focus();
		newRestProjectDialog.textBox().click();
		newRestProjectDialog.textBox().setText( "http://soapui.org" );

		JButtonFixture buttonOK = newRestProjectDialog.button( buttonWithText( "OK" ) );
		buttonOK.click();
	}
}
