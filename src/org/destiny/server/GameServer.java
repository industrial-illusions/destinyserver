package org.destiny.server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.destiny.server.connections.ActiveConnections;
import org.destiny.server.network.MySqlManager;
import org.ini4j.Ini;
import org.ini4j.Ini.Section;
import org.ini4j.InvalidIniFormatException;

/**
 * Represents a game server.
 * Starting a server requires a parameter to be passed in, i.e. java server.jar -ng -ar
 * Code is written/being re-written with intention of being compiled/ran with Java 8 u45 (1.8.0_45)
 * 
 * @author shadowkanji
 * @author Nushio
 * @author Akkarinage
 */
public class GameServer
{
	/* The revision of the game server */
	private static final int SERVER_REVISION = 0;
	private static String SERVER_HASH = "Unknown";
	public static final int MOVEMENT_THREADS = 12;
	private static boolean m_boolGui = false;
	private static String m_dbServer, m_dbName, m_dbUsername, m_dbPassword, m_serverName;
	private static GameServer m_instance;
	private static int m_maxPlayers = 100;
	private static ServiceManager m_serviceManager;
	private static int m_port = 7002;
	private JFrame m_gui;
	private JLabel m_pAmount, m_pHighest;
	private JButton m_start, m_stop, m_set, m_exit;
	private JPasswordField m_dbP;
	private JTextField m_dbS, m_dbN, m_dbU, m_name;
	private int m_highest;

	public static double RATE_GOLD = 1.0;
	public static double RATE_GOLD_VIP = 1.5;
	public static double RATE_EXP_POKE = 1.0;
	public static double RATE_EXP_POKE_VIP = 1.5;
	public static double RATE_EXP_TRAINER = 1.0;
	public static double RATE_EXP_TRAINER_VIP = 1.0;
	public static int RATE_WILDBATTLE = 8;
	public static int RATE_KICKDELAY = 30;
	public static final int AUTOSAVE_INTERVAL = 2; // Autosave interval in minutes
	public static int REVISION = getServerRevision();

	/**
	 * Default constructor.
	 * Starts the server after checking if it runs from command line or GUI.
	 * It automatically loads settings if possible.
	 * 
	 * @param autorun True if the server should autostart, otherwise false.
	 */
	public GameServer(boolean autorun)
	{
		if(autorun)
		{
			if(m_boolGui)
				createGui();
			loadSettings();
		}
		else if(m_boolGui)
		{
			loadSettings();
			createGui();
		}
		else
		{
			ConsoleReader r = new ConsoleReader();
			System.out.println("Load Settings? Y/N");
			String answer = r.readToken();
			if(answer.contains("y") || answer.contains("Y"))
				loadSettings();
			else
				getConsoleSettings();
		}
		start();
	}

	/**
	 * Updates the player count information.
	 */
	public void updatePlayerCount()
	{
		int amount = ActiveConnections.getActiveConnections();
		if(m_boolGui)
		{
			m_pAmount.setText(amount + " players online");
			if(amount > m_highest)
			{
				m_highest = amount;
				m_pHighest.setText("Highest: " + amount);
			}
		}
		else
		{
			System.out.println(amount + " players online");
			if(amount > m_highest)
			{
				m_highest = amount;
				System.out.println("Highest: " + amount);
			}
		}
	}

	/**
	 * Returns the database host.
	 * 
	 * @return The hostname or IP address of the database to connect to.
	 */
	public static String getDatabaseHost()
	{
		return m_dbServer;
	}

	/**
	 * Returns the selected database.
	 * 
	 * @return The name of the database to connect to.
	 */
	public static String getDatabaseName()
	{
		return m_dbName;
	}

	/**
	 * Returns the database password.
	 * 
	 * @return The password for the database.
	 */
	public static String getDatabasePassword()
	{
		return m_dbPassword;
	}

	/**
	 * Returns the database username.
	 * 
	 * @return The username for the database.
	 */
	public static String getDatabaseUsername()
	{
		return m_dbUsername;
	}

	/**
	 * Returns the instance of game server.
	 * 
	 * @return An instance of the GameServer.
	 */
	public static GameServer getInstance()
	{
		return m_instance;
	}

	/**
	 * Initializes the gameserver object.
	 * 
	 * @param autorun True if the server should autostart, otherwise false.
	 */
	public static void initGameServer(boolean autorun)
	{
		m_instance = new GameServer(autorun);
	}

	/**
	 * Returns the amount of players this server will allow.
	 * 
	 * @return The maximum amount of players simultaneously allowed.
	 */
	public static int getMaxPlayers()
	{
		return m_maxPlayers;
	}

	/**
	 * Returns the connection port for this server.
	 * 
	 * @return The port clients should connect to.
	 */
	public static int getPort()
	{
		return m_port;
	}

	/**
	 * Returns the name of this server.
	 * 
	 * @return The server's name.
	 */
	public static String getServerName()
	{
		return m_serverName;
	}

	/**
	 * Returns the service manager of the server.
	 * 
	 * @return
	 */
	public static ServiceManager getServiceManager()
	{
		return m_serviceManager;
	}

	/**
	 * The main entry point for the application.
	 * Reads the commandline arguments and starts the server.
	 * 
	 * @param args Optional commandline arguments.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidIniFormatException 
	 */
	public static void main(String[] args) throws InvalidIniFormatException, FileNotFoundException, IOException
	{
		/* Pipe errors to a file. */
		try
		{
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date date = new Date();
			PrintStream errorPrinter = new PrintStream(new File("logs/" + dateFormat.format(date) + ".txt"));
			System.setErr(errorPrinter);
		}
		catch(IOException | SecurityException e)
		{
			e.printStackTrace();
		}
		/* Server settings */
		Options options = new Options();
		options.addOption("ng", "nogui", false, "Starts server in headless mode.");
		options.addOption("ar", "autorun", false, "Runs without asking a single question.");
		if(args.length > 0)
		{
			CommandLineParser parser = new GnuParser();
			try
			{
				/* Parse the command line arguments. */
				CommandLine line = parser.parse(options, args);
				/* Create the server gui */
				if(!line.hasOption("nogui")){
					m_boolGui = true;
				}
				/* No else since it's set to default 'false'. */
				boolean autorun = line.hasOption("autorun");
				GameServer.initGameServer(autorun);
			}
			catch(ParseException pe)
			{
				/* Oops, something went wrong, automatically generate the help statement. */
				System.err.println("Parsing failed.  Reason: " + pe.getMessage());
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java GameServer [param] <args>", options);
			}
			Ini ratesIni = new Ini(new FileInputStream("conf/settings.ini"));
			Section s = ratesIni.get("RATES");
			RATE_GOLD = Double.parseDouble(s.get("GOLD"));
			RATE_GOLD_VIP = Double.parseDouble(s.get("GOLD_VIP"));
			RATE_EXP_POKE = Double.parseDouble(s.get("EXP_POKE"));
			RATE_EXP_POKE_VIP = Double.parseDouble(s.get("EXP_POKE_VIP"));
			RATE_EXP_TRAINER = Double.parseDouble(s.get("EXP_TRAINER"));
			RATE_EXP_TRAINER_VIP = Double.parseDouble(s.get("EXP_TRAINER_VIP"));
			RATE_WILDBATTLE = Integer.parseInt(s.get("WILDBATTLE"));
			RATE_KICKDELAY = Integer.parseInt(s.get("KICKDELAY"));
			Section server = ratesIni.get("SERVER");
			m_port = Integer.parseInt(server.get("PORT"));
			m_maxPlayers = Integer.parseInt(server.get("MAX_PLAYERS"));
		}
		else
		{
			/* Automatically generate the help statement. */
			HelpFormatter formatter = new HelpFormatter();
			System.err.println("Server requires a settings parameter");
			formatter.printHelp("java GameServer [param] <args>", options);
		}
	}

	
	
	/**
	 * Gets the GIT Hash of the server
	 * @author Akkarinage
	 * 
	 * @return hash
	 */
	private static String getServerHash()
	{
		String hash = SERVER_HASH;
		File file = new File(".git/refs/heads/master");
		try(Scanner sc = new Scanner(file))
		{
			hash = sc.nextLine();
		}
		catch(FileNotFoundException fnfe)
		{
			fnfe.printStackTrace();
		}
		
		return hash;
	}
	
	

	/**
	 * Starts the GameServer.
	 * This function fills the GUI fields (if any), checks the database connection and starts the servicemanager.
	 * Once the servicemanager is started, the server will start booting up.
	 **/
	public void start()
	{
		System.out.println("Git Hash: "+ getServerHash());
		if(m_boolGui)
		{
			m_dbServer = m_dbS.getText();
			m_dbName = m_dbN.getText();
			m_dbUsername = m_dbU.getText();
			m_dbPassword = new String(m_dbP.getPassword());
			m_serverName = m_name.getText();

			m_start.setEnabled(false);
			m_stop.setEnabled(true);
		}
		MySqlManager.getInstance();
		m_serviceManager = new ServiceManager();
		m_serviceManager.start();
	}

	/**
	 * Stops the GameServer.
	 * This function stops the servicemanager and waits for the processes to terminate.
	 * Finally, the database connection is closed and the application shutdown.
	 **/
	public void stop()
	{
		m_serviceManager.stop();
		if(m_boolGui)
		{
			m_start.setEnabled(true);
			m_stop.setEnabled(false);
		}
		else
		{
			try
			{
				Thread.sleep(10 * 1000);
				System.out.println("Exiting server...");
				MySqlManager.getInstance().close();
				System.exit(0);
			}
			catch(InterruptedException ie)
			{
				ie.printStackTrace();
			}
		}
	}

	/**
	 * Creates the gui-version of the server.
	 */
	private void createGui()
	{
		m_gui = new JFrame();
		m_gui.setTitle("Pokemon Destiny Server");
		m_gui.setSize(148, 340);
		m_gui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		m_gui.getContentPane().setLayout(null);
		m_gui.setResizable(false);
		m_gui.setLocation(32, 32);
		/* Set up the buttons */
		m_pAmount = new JLabel("0 players online");
		m_pAmount.setSize(160, 16);
		m_pAmount.setLocation(4, 4);
		m_pAmount.setVisible(true);
		m_gui.getContentPane().add(m_pAmount);

		m_pHighest = new JLabel("[No record]");
		m_pHighest.setSize(160, 16);
		m_pHighest.setLocation(4, 24);
		m_pHighest.setVisible(true);
		m_gui.getContentPane().add(m_pHighest);

		m_start = new JButton("Start Server");
		m_start.setSize(128, 24);
		m_start.setLocation(4, 48);
		m_start.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				start();
			}
		});
		m_gui.getContentPane().add(m_start);
		m_stop = new JButton("Stop Server");
		m_stop.setSize(128, 24);
		m_stop.setLocation(4, 74);
		m_stop.setEnabled(false);
		m_stop.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				stop();
			}
		});
		m_gui.getContentPane().add(m_stop);
		m_set = new JButton("Save Settings");
		m_set.setSize(128, 24);
		m_set.setLocation(4, 100);
		m_set.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				saveSettings();
			}
		});
		m_gui.getContentPane().add(m_set);
		m_exit = new JButton("Quit");
		m_exit.setSize(128, 24);
		m_exit.setLocation(4, 290);
		m_exit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0)
			{
				exit();
			}
		});
		m_gui.getContentPane().add(m_exit);
		/* Settings text boxes */
		m_dbS = new JTextField();
		m_dbS.setSize(128, 24);
		m_dbS.setText("MySQL Host");
		m_dbS.setLocation(4, 128);
		m_gui.getContentPane().add(m_dbS);

		m_dbN = new JTextField();
		m_dbN.setSize(128, 24);
		m_dbN.setText("MySQL Database Name");
		m_dbN.setLocation(4, 160);
		m_gui.getContentPane().add(m_dbN);

		m_dbU = new JTextField();
		m_dbU.setSize(128, 24);
		m_dbU.setText("MySQL Username");
		m_dbU.setLocation(4, 192);
		m_gui.getContentPane().add(m_dbU);

		m_dbP = new JPasswordField();
		m_dbP.setSize(128, 24);
		m_dbP.setText("Pass");
		m_dbP.setLocation(4, 224);
		m_gui.getContentPane().add(m_dbP);

		m_name = new JTextField();
		m_name.setSize(128, 24);
		m_name.setText("Your Server Name");
		m_name.setLocation(4, 260);
		m_gui.getContentPane().add(m_name);
		/* Load pre-existing settings if any */
		File f = new File("conf/database.txt");
		if(f.exists())
		{
			try(Scanner s = new Scanner(f))
			{
				m_dbS.setText(s.nextLine());
				m_dbN.setText(s.nextLine());
				m_dbU.setText(s.nextLine());
				m_dbP.setText(s.nextLine());
				m_name.setText(s.nextLine());
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
		m_gui.setVisible(true);
	}

	/** Exits the game server application. */
	private void exit()
	{
		if(m_boolGui && m_stop.isEnabled())
			JOptionPane.showMessageDialog(null, "You must stop the server before exiting.");
		else
		{
			MySqlManager.getInstance().close();
			System.exit(0);
		}
	}

	/**
	 * Asks for Database User/Pass, then asks to save
	 * NOTE: It doesnt save the database password.
	 **/
	private void getConsoleSettings()
	{
		ConsoleReader r = new ConsoleReader();
		System.out.println("Please enter the required information.");
		System.out.println("Database Server: ");
		m_dbServer = r.readToken();
		System.out.println("Database Name:");
		m_dbName = r.readToken();
		System.out.println("Database Username:");
		m_dbUsername = r.readToken();
		System.out.println("Database Password:");
		m_dbPassword = r.readToken();
		System.out.println("This server's IP or hostname:");
		m_serverName = r.readToken();
		System.out.println("Save info? (Y/N)");
		String answer = r.readToken();
		if(answer.contains("y") || answer.contains("Y"))
			saveSettings();
		System.out.println();
		System.err.println("WARNING: When using -nogui, the server should only be shut down using a master client");
	}

	/**
	 * Load pre-existing settings if any are available.
	 * NOTE: It loads the database password if available.
	 **/
	private void loadSettings()
	{
		File settings = new File("conf/database.txt");
		if(settings.exists())
		{
			try(Scanner s = new Scanner(settings))
			{
				m_dbServer = s.nextLine();
				m_dbName = s.nextLine();
				m_dbUsername = s.nextLine();
				m_dbPassword = s.nextLine();
				m_serverName = s.nextLine();
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}

	/**
	 * Writes server settings to a file.
	 * NOTE: It never stores the database password.
	 **/
	private void saveSettings()
	{
		/* Store globally */
		if(m_boolGui)
		{
			m_dbServer = m_dbS.getText();
			m_dbName = m_dbN.getText();
			m_dbUsername = m_dbU.getText();
			m_dbPassword = new String(m_dbP.getPassword());
			m_serverName = m_name.getText();
		}
		/* Write settings to file */
		File settings = new File("conf/database.txt");
		if(settings.exists())
			settings.delete();
		try(PrintWriter settingsWriter = new PrintWriter(settings))
		{
			settingsWriter.println(m_dbServer);
			settingsWriter.println(m_dbName);
			settingsWriter.println(m_dbUsername);
			settingsWriter.println(m_serverName);
			settingsWriter.println(" ");
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}
