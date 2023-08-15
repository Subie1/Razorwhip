package org.asf.razorwhip.sentinel.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;

import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;
import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.FlowLayout;

public class LauncherMain {

	public static final String LAUNCHER_VERSION = "1.0.0.A1";

	private JFrame frmSentinelLauncher;
	private JLabel lblStatusLabel;
	private boolean shiftDown;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		LauncherUtils.args = args;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LauncherMain window = new LauncherMain();
					window.frmSentinelLauncher.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public LauncherMain() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		try {
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e1) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {
		}

		frmSentinelLauncher = new JFrame();
		frmSentinelLauncher.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SHIFT)
					shiftDown = true;
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SHIFT)
					shiftDown = false;
			}
		});
		frmSentinelLauncher.setResizable(false);
		frmSentinelLauncher.setBounds(100, 100, 678, 262);
		frmSentinelLauncher.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmSentinelLauncher.setLocationRelativeTo(null);

		BackgroundPanel panel_1 = new BackgroundPanel();
		panel_1.setForeground(Color.WHITE);
		frmSentinelLauncher.getContentPane().add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));

		JPanel panel_4 = new JPanel();
		panel_4.setPreferredSize(new Dimension(10, 30));
		panel_1.add(panel_4, BorderLayout.SOUTH);
		panel_4.setBackground(new Color(10, 10, 10, 100));
		panel_4.setLayout(new BorderLayout(0, 0));

		lblStatusLabel = new JLabel("New label");
		panel_4.add(lblStatusLabel, BorderLayout.CENTER);
		lblStatusLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblStatusLabel.setForeground(Color.WHITE);
		lblStatusLabel.setPreferredSize(new Dimension(46, 20));

		JPanel panel_5 = new JPanel();
		panel_5.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_5.setPreferredSize(new Dimension(200, 10));
		panel_5.setBackground(new Color(255, 255, 255, 0));
		panel_4.add(panel_5, BorderLayout.EAST);
		panel_5.setLayout(new BoxLayout(panel_5, BoxLayout.X_AXIS));

		JProgressBar progressBar = new JProgressBar();
		panel_5.add(progressBar);
		progressBar.setPreferredSize(new Dimension(500, 14));
		progressBar.setBackground(new Color(240, 240, 240, 100));

		JPanel panelLabels = new JPanel();
		panelLabels.setBackground(Color.LIGHT_GRAY);
		panel_1.add(panelLabels, BorderLayout.CENTER);
		panelLabels.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel_2 = new JPanel();
		panel_2.setBackground(Color.LIGHT_GRAY);
		panel_2.setPreferredSize(new Dimension(600, 180));
		panelLabels.add(panel_2);
		panel_2.setLayout(null);

		JLabel lblNewLabel = new JLabel("<Project Name>");
		lblNewLabel.setBounds(12, 85, 576, 33);
		panel_2.add(lblNewLabel);
		lblNewLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setFont(new Font("Dialog", Font.PLAIN, 18));
		panel_5.setVisible(false);

		// Prepare launcher
		boolean overrodeSGD;
		boolean overrodeSVP;
		String softwareSourceClass;
		String descriptorSourceClass;
		File gameDescriptorFile = new File("gamedescriptor.sgd");
		File emulationSoftwareFile = new File("emulationsoftware.svp");
		boolean dirModeDescriptorFile = false;
		boolean dirModeSoftwareFile = false;
		String urlBaseDescriptorFile;
		String urlBaseSoftwareFile;
		Map<String, String> gameDescriptor;
		Map<String, String> softwareDescriptor;
		try {
			// Check debug
			LauncherUtils.log("Loading debug settings...");
			if (System.getProperty("debugGameDescriptorHintClass") != null) {
				// Find class
				LauncherUtils.log("Loading debug descriptor...");
				Class<?> cls = Class.forName(System.getProperty("debugGameDescriptorHintClass"));
				URL loc = cls.getProtectionDomain().getCodeSource().getLocation();
				File f = new File(loc.toURI());

				// Check file
				if (f.isDirectory()) {
					dirModeDescriptorFile = true;
				}
				gameDescriptorFile = f;
				LauncherUtils.log("Using descriptor: " + f);
			}
			if (System.getProperty("debugEmulationSoftwareHintClass") != null) {
				// Find class
				LauncherUtils.log("Loading debug emulation software...");
				Class<?> cls = Class.forName(System.getProperty("debugEmulationSoftwareHintClass"));
				URL loc = cls.getProtectionDomain().getCodeSource().getLocation();
				File f = new File(loc.toURI());

				// Check file
				if (f.isDirectory()) {
					dirModeSoftwareFile = true;
				}
				emulationSoftwareFile = f;
				LauncherUtils.log("Using emulation software: " + f);
			}

			// Check overrides
			if (new File("newgamedescriptor.sgd").exists()) {
				// Rename old file
				new File("gamedescriptor.sgd").renameTo(new File("gamedescriptor.sgd.old"));
				new File("newgamedescriptor.sgd").renameTo(new File("gamedescriptor.sgd"));

				// Update
				LauncherUtils.extractGameDescriptor(new File("gamedescriptor.sgd"), "overridden version");

				overrodeSGD = true;
			} else
				overrodeSGD = false;
			if (new File("newemulationsoftware.svp").exists()) {
				// Rename old file
				new File("emulationsoftware.svp").renameTo(new File("emulationsoftware.svp.old"));
				new File("newemulationsoftware.svp").renameTo(new File("emulationsoftware.svp"));

				// Update
				LauncherUtils.extractEmulationSoftware(new File("emulationsoftware.svp"), "overridden version");

				overrodeSVP = true;
			} else
				overrodeSVP = false;

			// Assign core fields
			LauncherUtils.panel = panel_1;
			LauncherUtils.statusLabel = lblStatusLabel;
			LauncherUtils.progressPanel = panel_5;
			LauncherUtils.progressBar = progressBar;

			// Check files
			LauncherUtils.log("Checking files...");
			if (!gameDescriptorFile.exists() || !emulationSoftwareFile.exists()) {
				// Not set up
				JOptionPane.showMessageDialog(frmSentinelLauncher,
						"Missing launcher files, please run the installer first.", "Launcher Error",
						JOptionPane.ERROR_MESSAGE);
				System.exit(1);
				return;
			}

			// Prepare
			LauncherUtils.log("Preparing data source URLs...");
			if (!dirModeDescriptorFile)
				urlBaseDescriptorFile = "jar:" + gameDescriptorFile.toURI() + "!";
			else
				urlBaseDescriptorFile = gameDescriptorFile.toURI().toString();
			if (!urlBaseDescriptorFile.endsWith("/"))
				urlBaseDescriptorFile += "/";
			if (!dirModeSoftwareFile)
				urlBaseSoftwareFile = "jar:" + emulationSoftwareFile.toURI() + "!";
			else
				urlBaseSoftwareFile = emulationSoftwareFile.toURI().toString();
			if (!urlBaseSoftwareFile.endsWith("/"))
				urlBaseSoftwareFile += "/";

			// Read descriptor info
			LauncherUtils.log("Loading game descriptor information...");
			gameDescriptor = LauncherUtils.parseProperties(getStringFrom(gameDescriptorFile, "descriptorinfo"));
			descriptorSourceClass = gameDescriptor.get("Game-Descriptor-Class");
			LauncherUtils.gameID = gameDescriptor.get("Game-ID");
			if (descriptorSourceClass == null)
				throw new IOException("No descriptor class defined in game descriptor");
			if (LauncherUtils.gameID == null)
				throw new IOException("No game ID defined in game descriptor");

			// Read software info
			LauncherUtils.log("Loading emulation software information...");
			softwareDescriptor = LauncherUtils.parseProperties(getStringFrom(emulationSoftwareFile, "softwareinfo"));
			if (!softwareDescriptor.containsKey("Game-ID"))
				throw new IOException("No game ID defined in emulation software descriptor");
			softwareSourceClass = softwareDescriptor.get("Software-Class");
			LauncherUtils.softwareName = softwareDescriptor.get("Project-Name");
			LauncherUtils.softwareVersion = softwareDescriptor.get("Software-Version");
			LauncherUtils.softwareID = softwareDescriptor.get("Software-ID");
			if (softwareSourceClass == null)
				throw new IOException("No software class defined in emulation software descriptor");
			if (LauncherUtils.softwareID == null)
				throw new IOException("No software ID defined in emulation software descriptor");
			if (LauncherUtils.softwareVersion == null)
				throw new IOException("No software version defined in emulation software descriptor");
			if (LauncherUtils.softwareName == null)
				throw new IOException("No software name defined in emulation software descriptor");
			if (!softwareDescriptor.get("Game-ID").equalsIgnoreCase(LauncherUtils.getGameID()))
				throw new IllegalArgumentException("Emulation software '" + LauncherUtils.softwareName
						+ "' is incompatible with the current game.");

			// Set title
			LauncherUtils.log(LauncherUtils.softwareName + " Launcher v" + LAUNCHER_VERSION);
			LauncherUtils.log("Game ID: " + LauncherUtils.gameID);
			LauncherUtils.log("Emulation software ID: " + LauncherUtils.softwareID);
			LauncherUtils.log("Emulation software version: " + LauncherUtils.softwareVersion);
			frmSentinelLauncher.setTitle(LauncherUtils.softwareName + " Launcher");
			lblNewLabel.setText(LauncherUtils.softwareName);

			// Download banner and set image
			panelLabels.setVisible(false);
			try {
				setPanelImageFrom(emulationSoftwareFile, "banner.png", panel_1);
			} catch (IOException e) {
				try {
					setPanelImageFrom(emulationSoftwareFile, "banner.jpg", panel_1);
				} catch (IOException e2) {
					try {
						setPanelImageFrom(gameDescriptorFile, "banner.png", panel_1);
					} catch (IOException e3) {
						try {
							setPanelImageFrom(gameDescriptorFile, "banner.jpg", panel_1);
						} catch (IOException e4) {
							panelLabels.setVisible(true);
						}
					}
				}
			}
		} catch (Exception e) {
			String stackTrace = "";
			for (StackTraceElement ele : e.getStackTrace())
				stackTrace += "\n     At: " + ele;
			System.out.println("[LAUNCHER] [SENTINEL LAUNCHER] Error occurred: " + e + stackTrace);
			JOptionPane.showMessageDialog(frmSentinelLauncher,
					"An error occured while running the launcher.\nUnable to continue, the launcher will now close.\n\nError details: "
							+ e + stackTrace,
					"Launcher Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}

		// Run launcher
		String urlBaseSoftwareFileF = urlBaseSoftwareFile;
		String urlBaseDescriptorFileF = urlBaseDescriptorFile;
		boolean dirModeDescriptorFileF = dirModeDescriptorFile;
		boolean dirModeSoftwareFileF = dirModeSoftwareFile;
		Thread th = new Thread(() -> {
			// Set progress bar status
			try {
				// Set label
				LauncherUtils.log("Preparing launcher...", true);
				LauncherUtils.resetProgressBar();

				boolean updatedSoftware = false;
				boolean updatedDescriptor = false;
				String descriptorClsName = descriptorSourceClass;
				String softwareClsName = softwareSourceClass;
				if (overrodeSVP)
					updatedSoftware = true;
				if (overrodeSGD)
					updatedDescriptor = true;

				// Check for game descriptor updates
				if (!dirModeDescriptorFileF) {
					if (gameDescriptor.containsKey("Update-List-URL")
							&& gameDescriptor.containsKey("Game-Descriptor-Version")) {
						LauncherUtils.log("Checking for game descriptor updates...");

						// Download list
						try {
							String lst = null;
							try {
								lst = downloadString(gameDescriptor.get("Update-List-URL"));
							} catch (IOException e) {
								LauncherUtils.log("Could not download update list!");
							}
							if (lst != null) {
								JsonObject list = JsonParser.parseString(lst).getAsJsonObject();
								String latest = list.get("latest").getAsString();
								String current = gameDescriptor.get("Game-Descriptor-Version");

								// Check
								if (!latest.equals(current)) {
									// Update
									LauncherUtils.log("Updating game descriptor...", true);
									LauncherUtils.log("Updating to " + latest + "...");
									JsonObject versionData = list.get("versions").getAsJsonObject().get(latest)
											.getAsJsonObject();
									String url = versionData.get("url").getAsString();
									LauncherUtils.resetProgressBar();
									LauncherUtils.downloadFile(url, new File("gamedescriptor.sgd.tmp"));

									// Load hashes
									String remoteHash = versionData.get("hash").getAsString();
									String localHash = LauncherUtils
											.sha256Hash(Files.readAllBytes(Path.of("gamedescriptor.sgd.tmp")));

									// Verify hashes
									boolean hashSuccess = true;
									if (!localHash.equals(remoteHash)) {
										// Redownload
										LauncherUtils.resetProgressBar();
										LauncherUtils.downloadFile(url, new File("gamedescriptor.sgd.tmp"));

										// Recheck
										localHash = LauncherUtils
												.sha256Hash(Files.readAllBytes(Path.of("gamedescriptor.sgd.tmp")));
										if (!localHash.equals(remoteHash)) {
											// Integrity check failure
											new File("gamedescriptor.sgd.tmp").delete();
											if (JOptionPane.showConfirmDialog(frmSentinelLauncher,
													"Failed to verify the integrity of the downloaded game descriptor file, the update will not be applied!\n"
															+ "\nThe launcher will continue with version " + current
															+ ", if you cancel, the launcher will be closed.",
													"Integrity check failure", JOptionPane.OK_CANCEL_OPTION,
													JOptionPane.ERROR_MESSAGE) == JOptionPane.CANCEL_OPTION) {
												System.exit(1);
											}
											hashSuccess = false;
										}
									}

									// Verify signature
									if (hashSuccess) {
										LauncherUtils.log("Verifying signature...", true);
										if (!LauncherUtils.verifyPackageSignature(new File("gamedescriptor.sgd.tmp"),
												new File("gamedescriptor-publickey.pem"))) {
											// Warn
											while (true) {
												if (JOptionPane.showConfirmDialog(frmSentinelLauncher,
														"WARNING! Failed to verify package signature!\n" //
																+ "\n" //
																+ "Failed to verify the update of the game descriptor file!\n"
																+ "The file that was downloaded may have been tampered with, proceed with caution!\n"
																+ "\n"
																+ "It is recommended to contact the developers if possible and ask them if the keys have changed between versions "
																+ current + " and " + latest + ".\n" //
																+ "\n" //
																+ "Do you wish to continue with the update? Selecting no will cancel the update." //
														, "Warning", JOptionPane.YES_NO_OPTION,
														JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
													if (JOptionPane.showConfirmDialog(frmSentinelLauncher,
															"Are you sure you want to ignore the file signature?",
															"Warning", JOptionPane.YES_NO_OPTION,
															JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
														break;
													} else
														continue;
												}
												hashSuccess = false;
												break;
											}
										}
									}

									// Check success
									if (hashSuccess) {
										// Rename old file
										new File("gamedescriptor.sgd").renameTo(new File("gamedescriptor.sgd.old"));
										new File("gamedescriptor.sgd.tmp").renameTo(new File("gamedescriptor.sgd"));
										updatedDescriptor = true;

										// Update
										LauncherUtils.extractGameDescriptor(new File("gamedescriptor.sgd"), latest);

										// Reload
										LauncherUtils.log("Reloading launcher...", true);
										LauncherUtils.hideProgressPanel();
										LauncherUtils.resetProgressBar();

										// Read descriptor info
										LauncherUtils.log("Loading game descriptor information...");
										gameDescriptor.clear();
										gameDescriptor.putAll(LauncherUtils.parseProperties(
												getStringFrom(new File("gamedescriptor.sgd"), "descriptorinfo")));
										descriptorClsName = gameDescriptor.get("Game-Descriptor-Class");
										LauncherUtils.gameID = gameDescriptor.get("Game-ID");
										if (descriptorClsName == null)
											throw new IOException("No descriptor class defined in game descriptor");
										if (LauncherUtils.gameID == null)
											throw new IOException("No game ID defined in game descriptor");
										LauncherUtils.log("Updated game descriptor to " + latest + "!");
									}
								}
							}
						} catch (Exception e) {
							try {
								if (updatedDescriptor) {
									// Restore
									updatedDescriptor = false;
									new File("gamedescriptor.sgd").delete();
									new File("gamedescriptor.sgd.old").renameTo(new File("gamedescriptor.sgd"));
								}
								SwingUtilities.invokeAndWait(() -> {
									String stackTrace = "";
									for (StackTraceElement ele : e.getStackTrace())
										stackTrace += "\n     At: " + ele;
									System.out.println(
											"[LAUNCHER] [SENTINEL LAUNCHER] Error occurred: " + e + stackTrace);
									JOptionPane.showMessageDialog(frmSentinelLauncher,
											"An error occured while updating the game descriptor file.\n\nError details: "
													+ e + stackTrace + "\n\nThe update has been cancelled.",
											"Update Error", JOptionPane.ERROR_MESSAGE);
								});
							} catch (InvocationTargetException | InterruptedException e1) {
							}
						}
						LauncherUtils.setStatus("Preparing launcher...");
					}
				}

				// Check for software updates
				if (!dirModeSoftwareFileF) {
					if (softwareDescriptor.containsKey("Update-List-URL")) {
						LauncherUtils.log("Checking for software updates...");

						// Download list
						try {
							String lst = null;
							try {
								lst = downloadString(softwareDescriptor.get("Update-List-URL"));
							} catch (IOException e) {
								LauncherUtils.log("Could not download update list!");
							}
							if (lst != null) {
								JsonObject list = JsonParser.parseString(lst).getAsJsonObject();
								String latest = list.get("latest").getAsString();

								// Check
								if (!latest.equals(LauncherUtils.softwareVersion)) {
									// Update
									LauncherUtils.log("Updating emulation software...", true);
									LauncherUtils.log("Updating to " + latest + "...");
									JsonObject versionData = list.get("versions").getAsJsonObject().get(latest)
											.getAsJsonObject();
									String url = versionData.get("url").getAsString();
									LauncherUtils.resetProgressBar();
									LauncherUtils.downloadFile(url, new File("emulationsoftware.svp.tmp"));

									// Load hashes
									String remoteHash = versionData.get("hash").getAsString();
									String localHash = LauncherUtils
											.sha256Hash(Files.readAllBytes(Path.of("emulationsoftware.svp.tmp")));

									// Verify hashes
									boolean hashSuccess = true;
									if (!localHash.equals(remoteHash)) {
										// Redownload
										LauncherUtils.resetProgressBar();
										LauncherUtils.downloadFile(url, new File("emulationsoftware.svp.tmp"));

										// Recheck
										localHash = LauncherUtils
												.sha256Hash(Files.readAllBytes(Path.of("emulationsoftware.svp.tmp")));
										if (!localHash.equals(remoteHash)) {
											// Integrity check failure
											new File("emulationsoftware.svp.tmp").delete();
											if (JOptionPane.showConfirmDialog(frmSentinelLauncher,
													"Failed to verify the integrity of the downloaded emulation software update, the update will not be applied!\n"
															+ "\nThe launcher will continue with version "
															+ LauncherUtils.softwareVersion
															+ ", if you cancel, the launcher will be closed.",
													"Integrity check failure", JOptionPane.OK_CANCEL_OPTION,
													JOptionPane.ERROR_MESSAGE) == JOptionPane.CANCEL_OPTION) {
												System.exit(1);
											}
											hashSuccess = false;
										}
									}

									// Verify signature
									if (hashSuccess) {
										LauncherUtils.log("Verifying signature...", true);
										if (!LauncherUtils.verifyPackageSignature(new File("emulationsoftware.svp.tmp"),
												new File("emulationsoftware-publickey.pem"))) {
											// Warn
											while (true) {
												if (JOptionPane.showConfirmDialog(frmSentinelLauncher,
														"WARNING! Failed to verify package signature!\n" //
																+ "\n" //
																+ "Failed to verify the update of the emulation software package '"
																+ LauncherUtils.softwareName + "'!\n"
																+ "The file that was downloaded may have been tampered with, proceed with caution!\n"
																+ "\n"
																+ "It is recommended to contact the developers if possible and ask them if the keys have changed between versions "
																+ LauncherUtils.softwareVersion + " and " + latest
																+ ".\n" //
																+ "\n" //
																+ "Do you wish to continue with the update? Selecting no will cancel the update." //
														, "Warning", JOptionPane.YES_NO_OPTION,
														JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
													if (JOptionPane.showConfirmDialog(frmSentinelLauncher,
															"Are you sure you want to ignore the file signature?",
															"Warning", JOptionPane.YES_NO_OPTION,
															JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
														break;
													} else
														continue;
												}
												hashSuccess = false;
												break;
											}
										}
									}

									// Check success
									if (hashSuccess) {
										// Rename old file
										new File("emulationsoftware.svp")
												.renameTo(new File("emulationsoftware.svp.old"));
										new File("emulationsoftware.svp.tmp")
												.renameTo(new File("emulationsoftware.svp"));
										updatedSoftware = true;

										// Update
										LauncherUtils.extractEmulationSoftware(new File("emulationsoftware.svp"),
												latest);

										// Reload
										LauncherUtils.log("Reloading launcher...", true);
										LauncherUtils.hideProgressPanel();
										LauncherUtils.resetProgressBar();
										softwareDescriptor.clear();
										softwareDescriptor.putAll(LauncherUtils.parseProperties(
												getStringFrom(new File("emulationsoftware.svp"), "softwareinfo")));
										if (!softwareDescriptor.containsKey("Game-ID"))
											throw new IOException(
													"No game ID defined in emulation software descriptor");
										LauncherUtils.softwareName = softwareDescriptor.get("Project-Name");
										softwareClsName = softwareDescriptor.get("Software-Class");
										LauncherUtils.softwareVersion = softwareDescriptor.get("Software-Version");
										LauncherUtils.softwareID = softwareDescriptor.get("Software-ID");
										if (softwareClsName == null)
											throw new IOException(
													"No software class defined in emulation software descriptor");
										if (LauncherUtils.softwareID == null)
											throw new IOException(
													"No software ID defined in emulation software descriptor");
										if (LauncherUtils.softwareVersion == null)
											throw new IOException(
													"No software version defined in emulation software descriptor");
										if (LauncherUtils.softwareName == null)
											throw new IOException(
													"No software name defined in emulation software descriptor");
										if (!softwareDescriptor.get("Game-ID")
												.equalsIgnoreCase(LauncherUtils.getGameID()))
											throw new IllegalArgumentException(
													"Emulation software '" + LauncherUtils.softwareName
															+ "' is incompatible with the current game.");
										LauncherUtils
												.log(LauncherUtils.softwareName + " Launcher v" + LAUNCHER_VERSION);
										LauncherUtils.log("Game ID: " + LauncherUtils.gameID);
										LauncherUtils.log("Emulation software ID: " + LauncherUtils.softwareID);
										LauncherUtils
												.log("Emulation software version: " + LauncherUtils.softwareVersion);
										SwingUtilities.invokeAndWait(() -> {
											frmSentinelLauncher.setTitle(LauncherUtils.softwareName + " Launcher");
											lblNewLabel.setText(LauncherUtils.softwareName);

											// Download banner and set image
											panelLabels.setVisible(false);
											try {
												setPanelImageFrom(new File("emulationsoftware.svp"), "banner.png",
														panel_1);
											} catch (IOException e) {
												try {
													setPanelImageFrom(new File("emulationsoftware.svp"), "banner.jpg",
															panel_1);
												} catch (IOException e2) {
													try {
														setPanelImageFrom(new File("gamedescriptor.sgd"), "banner.png",
																panel_1);
													} catch (IOException e3) {
														try {
															setPanelImageFrom(new File("gamedescriptor.sgd"),
																	"banner.jpg", panel_1);
														} catch (IOException e4) {
															panelLabels.setVisible(true);
														}
													}
												}
											}
										});
										LauncherUtils.log("Updated emulation software to " + latest + "!");
									}
								}
							}
						} catch (Exception e) {
							try {
								if (updatedSoftware) {
									// Restore
									updatedSoftware = false;
									new File("emulationsoftware.svp").delete();
									new File("emulationsoftware.svp.old").renameTo(new File("emulationsoftware.svp"));
								}
								SwingUtilities.invokeAndWait(() -> {
									String stackTrace = "";
									for (StackTraceElement ele : e.getStackTrace())
										stackTrace += "\n     At: " + ele;
									System.out.println(
											"[LAUNCHER] [SENTINEL LAUNCHER] Error occurred: " + e + stackTrace);
									JOptionPane.showMessageDialog(frmSentinelLauncher,
											"An error occured while updating emulation software.\n\nError details: " + e
													+ stackTrace + "\n\nThe update has been cancelled.",
											"Update Error", JOptionPane.ERROR_MESSAGE);
								});
							} catch (InvocationTargetException | InterruptedException e1) {
							}
						}
						LauncherUtils.setStatus("Preparing launcher...");
					}
				}

				// Re-extract software package if the descriptor updated but not the software
				if (updatedDescriptor && !updatedSoftware) {
					// Re-extract
					LauncherUtils.log("Re-extracting software...", true);
					LauncherUtils.resetProgressBar();
					LauncherUtils.showProgressPanel();

					// Update
					LauncherUtils.extractEmulationSoftware(new File("emulationsoftware.svp"),
							LauncherUtils.softwareVersion);

					// Reset
					LauncherUtils.setStatus("Preparing launcher...");
				}

				// Add to classpath
				LauncherUtils.addUrlToComponentClassLoader(new File("gamedescriptor.sgd").toURI().toURL());
				LauncherUtils.addUrlToComponentClassLoader(new File("emulationsoftware.svp").toURI().toURL());

				try {
					// Load object
					LauncherUtils.log("Loading game descriptor class...");
					Class<?> gameDescr = LauncherUtils.loader.loadClass(descriptorClsName);
					if (!IGameDescriptor.class.isAssignableFrom(gameDescr))
						throw new IllegalArgumentException(
								descriptorClsName + " does not implement " + IGameDescriptor.class.getTypeName());
					Constructor<?> gdCt = gameDescr.getConstructor();
					LauncherUtils.gameDescriptor = (IGameDescriptor) gdCt.newInstance();
					LauncherUtils.gameDescriptor.init();

					// Apply update
					if (updatedDescriptor) {
						new File("gamedescriptor.sgd.old").delete();
						LauncherUtils.gameDescriptor.postUpdate();
					}
				} catch (Exception e) {
					if (updatedDescriptor) {
						// Restore
						new File("gamedescriptor.sgd.old").renameTo(new File("newgamedescriptor.sgd"));
					}
					if (updatedSoftware) {
						// Restore
						new File("emulationsoftware.svp.old").renameTo(new File("newemulationsoftware.svp"));
					}
					throw e;
				}

				try {
					// Load object
					LauncherUtils.log("Loading emulation software provider class...");
					Class<?> softProv = LauncherUtils.loader.loadClass(softwareClsName);
					if (!IEmulationSoftwareProvider.class.isAssignableFrom(softProv))
						throw new IllegalArgumentException(softwareClsName + " does not implement "
								+ IEmulationSoftwareProvider.class.getTypeName());
					Constructor<?> sfCt = softProv.getConstructor();
					LauncherUtils.emulationSoftware = (IEmulationSoftwareProvider) sfCt.newInstance();
					LauncherUtils.emulationSoftware.init();

					// Apply update
					if (updatedSoftware) {
						new File("emulationsoftware.svp.old").delete();
						LauncherUtils.emulationSoftware.postUpdate();
					}
				} catch (Exception e) {
					if (updatedSoftware) {
						// Restore
						new File("emulationsoftware.svp.old").renameTo(new File("newemulationsoftware.svp"));
					}
					throw e;
				}

				// TODO: check asset archive data
				// Redownload if updated, download if not present (use setup wizard)
				// Redownload client too, re-extract SVP and SGD if needed

				// TODO: payloads

				// TODO: launcher logic
			} catch (Exception e) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						String stackTrace = "";
						for (StackTraceElement ele : e.getStackTrace())
							stackTrace += "\n     At: " + ele;
						System.out.println("[LAUNCHER] [SENTINEL LAUNCHER] Error occurred: " + e + stackTrace);
						JOptionPane.showMessageDialog(frmSentinelLauncher,
								"An error occured while running the launcher.\nUnable to continue, the launcher will now close.\n\nError details: "
										+ e + stackTrace,
								"Launcher Error", JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					});
				} catch (InvocationTargetException | InterruptedException e1) {
				}
			}
		}, "Launcher Thread");
		th.setDaemon(true);
		th.start();

	}

	private void setPanelImageFrom(File file, String entry, BackgroundPanel panel) throws IOException {
		if (file.isDirectory()) {
			FileInputStream strm = new FileInputStream(new File(file, entry));

			// Read
			BufferedImage img = ImageIO.read(strm);
			panel.setImage(img);

			// Close
			strm.close();
			return;
		}

		// Get zip
		ZipFile f = new ZipFile(file);
		try {
			ZipEntry ent = f.getEntry(entry);
			if (ent == null) {
				throw new FileNotFoundException("Entry " + entry + " not found in " + file);
			}

			// Get stream
			InputStream strm = f.getInputStream(ent);

			// Read
			BufferedImage img = ImageIO.read(strm);
			panel.setImage(img);

			// Close
			strm.close();
		} finally {
			f.close();
		}
	}

	private String getStringFrom(File file, String entry) throws IOException {
		if (file.isDirectory()) {
			FileInputStream strm = new FileInputStream(new File(file, entry));

			// Read
			String res = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Return
			return res;
		}

		// Get zip
		ZipFile f = new ZipFile(file);
		try {
			ZipEntry ent = f.getEntry(entry);
			if (ent == null) {
				throw new FileNotFoundException("Entry " + entry + " not found in " + file);
			}

			// Get stream
			InputStream strm = f.getInputStream(ent);

			// Read
			String res = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Return
			return res;
		} finally {
			f.close();
		}
	}

	private String downloadString(String url) throws IOException {
		URLConnection conn = new URL(url).openConnection();
		InputStream strm = conn.getInputStream();
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();
		return data;
	}

}
