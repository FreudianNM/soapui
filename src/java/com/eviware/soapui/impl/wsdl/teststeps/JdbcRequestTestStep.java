/*
 *  soapUI, copyright (C) 2004-2009 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.wsdl.teststeps;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.JdbcRequestTestStepConfig;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.WsdlSubmit;
import com.eviware.soapui.impl.wsdl.panels.support.MockTestRunContext;
import com.eviware.soapui.impl.wsdl.panels.support.MockTestRunner;
import com.eviware.soapui.impl.wsdl.support.assertions.AssertionsSupport;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestRunContext;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionRegistry.AssertableType;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.propertyexpansion.DefaultPropertyExpansionContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionsListener;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.support.DocumentListenerAdapter;
import com.eviware.soapui.support.PropertyChangeNotifier;
import com.eviware.soapui.support.SoapUIException;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.SimpleForm;
import com.eviware.soapui.support.swing.JXEditAreaPopupMenu;
import com.eviware.soapui.support.xml.JXEditTextArea;


/**
 * WsdlTestStep that executes a WsdlTestRequest
 * 
 * @author dragica.soldo
 */

public class JdbcRequestTestStep extends WsdlTestStepWithProperties implements Assertable
{
	private final static Logger log = Logger.getLogger(WsdlTestRequestStep.class);
	private JdbcRequestTestStepConfig jdbcRequestTestStepConfig;
	public final static String JDBCREQUEST = JdbcRequestTestStep.class.getName() + "@jdbcrequest";
	private WsdlSubmit<WsdlRequest> submit;
	private ImageIcon failedIcon;
	private ImageIcon okIcon;
   private String xmlResult;
   public String getXmlResult()
	{
		return xmlResult;
	}
   public void setXmlResult(String xmlResult)
	{
		this.xmlResult = xmlResult;
	}
   private boolean runnable;

	public boolean isRunnable()
	{
		return runnable;
	}
	protected static final String DRIVER_FIELD = "Driver";
	protected static final String CONNSTR_FIELD = "Connection String";
	protected static final String PASS_FIELD = "Password";
	public static final String QUERY_FIELD = "SQL Query";
	protected static final String STOREDPROCEDURE_FIELD = "Stored Procedure";
	protected static final String DATA_CONNECTION_FIELD = "Connection";

	protected static final String QUERY_ELEMENT = "query";
	protected static final String STOREDPROCEDURE_ELEMENT = "stored-procedure";
	protected JPanel panel;
	protected String driver;
	protected String connectionString;
	protected String password;
	protected String query;
	//for start set to false...later to be implemented
	protected boolean storedProcedure = false;
	protected SimpleForm form;
	protected Connection connection;
	protected JXEditTextArea queryArea;
	private JButton testConnectionButton;
	private AssertionsSupport assertionsSupport;

	public String getDriver()
	{
		return jdbcRequestTestStepConfig.getDriver();
	}

	public void setDriver(String d)
	{
		String old = getDriver();
		jdbcRequestTestStepConfig.setDriver(driver);
		notifyPropertyChanged( "driver", old, d );
	}

	public String getConnectionString()
	{
		return jdbcRequestTestStepConfig.getConnectionString();
	}

	public void setConnectionString(String c)
	{
		String old = getConnectionString();
		jdbcRequestTestStepConfig.setConnectionString(c);
		notifyPropertyChanged( "connectionString", old, c );
	}

	public String getQuery()
	{
		return jdbcRequestTestStepConfig.getQuery();
	}

	public void setQuery(String q)
	{
		String old = getQuery();
		jdbcRequestTestStepConfig.setQuery(q);
		notifyPropertyChanged( "query", old, q );
	}
	public JdbcRequestTestStepConfig getJdbcRequestTestStepConfig()
	{
		return jdbcRequestTestStepConfig;
	}

	public JdbcRequestTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest)
	{
		super(testCase, config, true, forLoadTest);

		if (!forLoadTest)
		{
			okIcon = UISupport.createImageIcon("/datasource.gif");
			failedIcon = UISupport.createImageIcon("/datasource_failed.gif");
			setIcon(okIcon);
		}
		if (getConfig().getConfig() != null)
		{
			jdbcRequestTestStepConfig = (JdbcRequestTestStepConfig) getConfig().getConfig().changeType(
					JdbcRequestTestStepConfig.type);

		}
		else
		{
			jdbcRequestTestStepConfig = (JdbcRequestTestStepConfig) getConfig().addNewConfig().changeType(
					JdbcRequestTestStepConfig.type);
		}
		driver = getDriver();
		connectionString = getConnectionString();
		query = getQuery();
	}


	public JComponent getComponent()
	{
		if (panel == null)
		{
			panel = new JPanel(new BorderLayout());

			form = new SimpleForm();
			form.addSpace(5);

			form.setDefaultTextFieldColumns(50);

			JTextField textField = form.appendTextField(DRIVER_FIELD, "JDBC Driver to use");
			textField.setText(getDriver());
			// PropertyExpansionPopupListener.enable( textField,
			// getDataSourceStep() );
			textField.getDocument().addDocumentListener(new DocumentListenerAdapter()
			{

				@Override
				public void update(Document document)
				{
					driver = form.getComponentValue(DRIVER_FIELD);
					setDriver(driver);
					if (StringUtils.isNullOrEmpty(driver) || StringUtils.isNullOrEmpty(connectionString))
					{
						testConnectionButton.setEnabled(false);
					}
					else
					{
						testConnectionButton.setEnabled(true);
					}
					if (StringUtils.isNullOrEmpty(driver) || StringUtils.isNullOrEmpty(connectionString) ||
							StringUtils.isNullOrEmpty(query))
					{
						runnable = false;
					}
					else
					{
						runnable = true;
					}
			}
			});

			textField = form.appendTextField(CONNSTR_FIELD, "JDBC Driver Connection String");
			textField.setText(getConnectionString());
			// PropertyExpansionPopupListener.enable( textField,
			// getDataSourceStep() );
			textField.getDocument().addDocumentListener(new DocumentListenerAdapter()
			{

				@Override
				public void update(Document document)
				{
					connectionString = form.getComponentValue(CONNSTR_FIELD);
					setConnectionString(connectionString);
					if (StringUtils.isNullOrEmpty(driver) || StringUtils.isNullOrEmpty(connectionString))
					{
						testConnectionButton.setEnabled(false);
					}
					else
					{
						testConnectionButton.setEnabled(true);
					}
					if (StringUtils.isNullOrEmpty(driver) || StringUtils.isNullOrEmpty(connectionString) ||
							StringUtils.isNullOrEmpty(query))
					{
						runnable = false;
					}
					else
					{
						runnable = true;
					}
				}
			});

			// JPasswordField passField = form.appendPasswordField(PASS_FIELD,
			// "Connection string Password");
			// passField.setText(password);
			// passField.getDocument().addDocumentListener(new
			// DocumentListenerAdapter() {
			//		
			// @Override
			// public void update(Document document) {
			// password = form
			// .getComponentValue(PASS_FIELD);
			// saveConfig();
			// if (StringUtils.isNullOrEmpty(driver) ||
			// StringUtils.isNullOrEmpty(connectionString) &&
			// (DatabaseConnection.isNeededPassword(connectionString) &&
			// StringUtils.isNullOrEmpty(password)))
			// {
			// testConnectionButton.setEnabled(false);
			// } else {
			// testConnectionButton.setEnabled(true);
			// }
			// }
			// });
			testConnectionButton = form.appendButton("TestConnection", "Test selected database connection");
			testConnectionButton.setAction(new TestConnectionAction());
			if (StringUtils.isNullOrEmpty(driver) || StringUtils.isNullOrEmpty(connectionString))
			{
				testConnectionButton.setEnabled(false);
			}
			else
			{
				testConnectionButton.setEnabled(true);
			}

			if (StringUtils.isNullOrEmpty(driver) || StringUtils.isNullOrEmpty(connectionString) ||
					StringUtils.isNullOrEmpty(query))
			{
				runnable = false;
			}
			else
			{
				runnable = true;
			}
			queryArea = JXEditTextArea.createSqlEditor();
			JXEditAreaPopupMenu.add(queryArea);
			// PropertyExpansionPopupListener.enable( queryArea,
			// getDataSourceStep() );
			queryArea.setText(getQuery());
			JScrollPane scrollPane = new JScrollPane(queryArea);
			scrollPane.setPreferredSize(new Dimension(400, 150));
			form.append(QUERY_FIELD, scrollPane);
			queryArea.getDocument().addDocumentListener(new DocumentListenerAdapter()
			{

				@Override
				public void update(Document document)
				{
					query = queryArea.getText();
					setQuery(query);
				}
			});

			// isStoredProcedureCheckBox = form.appendCheckBox(
			// STOREDPROCEDURE_FIELD,
			// "Select if this is a stored procedure", storedProcedure );
			// isStoredProcedureCheckBox.addChangeListener(
			// new ChangeListener()
			// {
			// public void stateChanged( ChangeEvent e )
			// {
			// storedProcedure = ( (JCheckBox) e.getSource() ).isSelected();
			// saveConfig();
			// }
			// } );

			panel.add(form.getPanel());
		}

		return panel;
	}

	@Override
	public WsdlTestStep clone(WsdlTestCase targetTestCase, String name)
	{
		beforeSave();

		TestStepConfig config = (TestStepConfig) getConfig().copy();
		JdbcRequestTestStepConfig stepConfig = (JdbcRequestTestStepConfig) config.getConfig().changeType(
				JdbcRequestTestStepConfig.type);

		JdbcRequestTestStep result = (JdbcRequestTestStep) targetTestCase.addTestStep(config);

		return result;
	}

	@Override
	public void release()
	{
		super.release();
	}

	public TestStepResult run(TestCaseRunner runner, TestCaseRunContext runContext)
	{
		WsdlTestStepResult testStepResult = new WsdlTestStepResult(this);

		return testStepResult;
	}

	@Override
	public boolean cancel()
	{
		if (submit == null)
			return false;

		submit.cancel();

		return true;
	}

	public String getDefaultSourcePropertyName()
	{
		return "Response";
	}

	public class TestConnectionAction extends AbstractAction
	{
		public TestConnectionAction()
		{
			putValue(Action.SMALL_ICON, UISupport.createImageIcon("/run_testcase.gif"));
			putValue(Action.SHORT_DESCRIPTION, "Test the current Connection");

			setEnabled(false);
		}

		public void actionPerformed(ActionEvent arg0)
		{
			 testDatabaseConnection(getModelItem(), driver, connectionString);
		}
	}

	public void testDatabaseConnection(ModelItem testingModelItem, String driver, String connectionString) {
		try {
			testConn(testingModelItem, driver, connectionString, password);
			UISupport.showInfoMessage("The Connection Successfully Tested");
		} catch (Exception e) {
			UISupport.showErrorMessage("Can't get the Connection for specified properties; " + e.toString());
		}
	}
	public static Connection testConn(ModelItem modelItem, String driver, String connectionString, String password)
			throws Exception, SQLException
	{
		PropertyExpansionContext context = new DefaultPropertyExpansionContext(modelItem);

//		String drvr = PropertyExpander.expandProperties(context, driver).trim();
//		String connStr = PropertyExpander.expandProperties(context, connectionString).trim();
		String drvr = driver;
		String connStr = connectionString;
		try
		{
			DriverManager.getDriver(connStr);
		}
		catch (SQLException e)
		{
			try
			{
				Class.forName(drvr).newInstance();
			}
			catch (Exception e1)
			{
				throw new Exception("Failed to init connection for drvr [" + drvr + "], connectionString [" + connectionString
						+ "]");
			}
		}
		return DriverManager.getConnection(connStr);

	}
	//running
   protected ResultSet resultSet;
   protected Statement statement;
   
	public void runQuery() {
		MockTestRunner mockRunner = new MockTestRunner( getTestCase() );
		MockTestRunContext mockContext = new MockTestRunContext( mockRunner, this);
		try
		{
			prepare( mockRunner, mockContext );
			List<String> properties = new ArrayList<String> ();
			load(mockRunner, mockContext, properties)	;	
			createXmlResult();
		}
		catch( Exception e )
		{
			UISupport.showErrorMessage( e );
		}
	}

   protected void getDatabaseConnection(PropertyExpansionContext context) throws Exception,
	SQLException {
	   String drvr ="";
	   String connStr="";
	   if (!StringUtils.isNullOrEmpty(driver) && !StringUtils.isNullOrEmpty(connectionString)) {
			 drvr = PropertyExpander.expandProperties( context, driver ).trim();
			 connStr = PropertyExpander.expandProperties( context, connectionString ).trim();
	   } else {
	   	UISupport.showErrorMessage( "Please supply connection settings for all DataSources" );
	   	throw new SoapUIException("Please supply connection settings");
	   }
//		String masskedPass = connStr.replace(DatabaseConnection.PASS_TEMPLATE, "#####"); 
//		connStr = connStr.replaceFirst(DatabaseConnection.PASS_TEMPLATE, password);
	     try
	     {
	        DriverManager.getDriver( connStr );
	     }
	     catch( SQLException e )
	     {
	        try
	        {
	           Class.forName( drvr ).newInstance();
	        }
	        catch( Exception e1 )
	        {
	           throw new Exception( "Failed to init connection for drvr [" + drvr + "], connectionString [" + connectionString + "]" );
	        }
	     }
	
	     resultSet = null;
	     connection = DriverManager.getConnection( connStr);
//	     lastResult = new StringToStringMap();
	}
   public void load( TestCaseRunner testRunner, TestCaseRunContext context, List<String> properties ) throws Exception
   {
      if( storedProcedure )
      {
         ( (CallableStatement) statement ).execute();
      }
      else
      {
         String q = PropertyExpander.expandProperties( context, query );
         statement.execute( q );
      }

      resultSet = statement.getResultSet();

//      getColumnNamesForCurrentResultSet();
//      resultSetCount = resultSet == null ? 0 : 1;
   }
   
   @SuppressWarnings( "unchecked" )
   @Override
   public void prepare( TestCaseRunner testRunner, TestCaseRunContext context) throws Exception
   {
      getDatabaseConnection(context);

      if( storedProcedure )
      {
         String sql = PropertyExpander.expandProperties( context, query );

         if( !sql.startsWith( "{call " ) && !sql.endsWith( "}" ) )
            sql = "{call " + sql + "}";

         statement = connection.prepareCall( sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY );
      }
      else
      {
         statement = connection.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY );
      }
      super.prepare( testRunner, context );   
    }
   
   public void createXmlResult() {
      ResultSet rs = resultSet;
      Statement stmt = statement;

      try {
        DocumentBuilderFactory factory = 
           DocumentBuilderFactory.newInstance();
        DocumentBuilder builder =factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();
        Element results = doc.createElement("Results");
        doc.appendChild(results);

        // connection to an ACCESS MDB
        ResultSetMetaData rsmd = rs.getMetaData();
        int colCount = rsmd.getColumnCount();

        while (rs.next()) {
          Element row = doc.createElement("Row");
          results.appendChild(row);
          for (int ii = 1; ii <= colCount; ii++) {
             String columnName = rsmd.getColumnName(ii);
             String value = rs.getString(ii);
             Element node = doc.createElement(columnName);
             if (!StringUtils.isNullOrEmpty(value))
				{
					node.appendChild(doc.createTextNode(value.toString()));
				}
				row.appendChild(node);
          }
        }

        String oldRes = getXmlResult();
        xmlResult = getDocumentAsXml(doc);
        notifyPropertyChanged("xmlResult", oldRes, xmlResult);

      }
      catch (Exception e) {
          e.printStackTrace();
      }
      finally {
        try {
          if (connection != null) connection.close();
          if (stmt != null) stmt.close();
          if (rs != null) rs.close();
        }
        catch (Exception e) {
        }
      }
   }
   public static String getDocumentAsXml(org.w3c.dom.Document doc)
   throws TransformerConfigurationException, TransformerException {
		 DOMSource domSource = new DOMSource(doc);
		 TransformerFactory tf = TransformerFactory.newInstance();
		 Transformer transformer = tf.newTransformer();
		 //transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		 transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		 transformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
		 // we want to pretty format the XML output
		 // note : this is broken in jdk1.5 beta!
		 transformer.setOutputProperty
		    ("{http://xml.apache.org/xslt}indent-amount", "4");
		 transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		 //
		 java.io.StringWriter sw = new java.io.StringWriter();
		 StreamResult sr = new StreamResult(sw);
		 transformer.transform(domSource, sr);
		 return sw.toString();
}
	public TestAssertion addAssertion(String assertionLabel)
	{
		// TODO Auto-generated method stub
		return null;
	}
	public void addAssertionsListener(AssertionsListener listener)
	{
		// TODO Auto-generated method stub
		
	}
	public TestAssertion cloneAssertion(TestAssertion source, String name)
	{
		// TODO Auto-generated method stub
		return null;
	}
	public String getAssertableContent()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public AssertableType getAssertableType()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public TestAssertion getAssertionAt(int c)
	{
		// TODO Auto-generated method stub
		return null;
	}
	public TestAssertion getAssertionByName(String name)
	{
		// TODO Auto-generated method stub
		return null;
	}
	public int getAssertionCount()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	public List<TestAssertion> getAssertionList()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public AssertionStatus getAssertionStatus()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public Map<String, TestAssertion> getAssertions()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public String getDefaultAssertableContent()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public Interface getInterface()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public TestAssertion moveAssertion(int ix, int offset)
	{
		// TODO Auto-generated method stub
		return null;
	}
	public void removeAssertion(TestAssertion assertion)
	{
		// TODO Auto-generated method stub
		
	}
	public void removeAssertionsListener(AssertionsListener listener)
	{
		// TODO Auto-generated method stub
		
	}
	

}
