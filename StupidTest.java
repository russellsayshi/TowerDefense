import javax.swing.*;
import java.awt.*;
import java.util.*;
import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.awt.event.*;

public class StupidTest extends JPanel {
	public BufferedImage[] images;
	public BufferedImage grass, cobblestone;
	public Rectangle[] path = new Rectangle[100];
	public Rectangle[] lines = new Rectangle[100];
	public boolean[] lineNormal = new boolean[100];
	public boolean[] longboys = new boolean[100];
	public int[] startx = new int[100];
	public int[] endx = new int[100];
	public int[] starty = new int[100];
	public int[] endy = new int[100];
	public volatile boolean frameDue = true;
	public String[] towers = {"Normal", "Archer", "Rapid", "Johnnyboy"};
	public int[] towerPrice = {50, 60, 80, 150};
	public int[] towerDamage = {30, 50, 70, 100};
	public int[] towerRecharge = {30, 20, 10, 25};
	public boolean won = false;
	public int[] towerRange = {100, 250, 120, 300};
	public String mode = "Build map";
	public boolean hasMadeRects = false;
	public int rectX, rectY;
	public boolean makingRect = false;
	int rectctr = 0;
	public static JFrame frame;
	public boolean hasDrawnWhite = false;
	public ArrayList<Troop> troops = new ArrayList<>();
	public int gold = 200;
	public int selectedTower = -1;
	public ArrayList<Tower> placedTowers = new ArrayList<>();
	public String errorMessage = "";
	public int errorCounter = 0;
	public int mouseXg, mouseYg;
	public int wave = 0;
	public boolean inRound = false;
	public ArrayList<Wave> waves;
	public ArrayList<Integer> troopsToAdd = new ArrayList<>();
	public ArrayList<Integer> troopSpeeds = new ArrayList<>();
	public ArrayList<Integer> troopHealth = new ArrayList<>();
	public ArrayList<BufferedImage> troopImage = new ArrayList<>();
	public boolean dead = false;
	public int lineLength = 0;
	public int lives = 10;

	public void readInTroopData() throws Exception {
		Scanner scan = new Scanner(new File("numtroops.txt"));
		int num = scan.nextInt();
		System.out.println("Looking for " + num + " troops.");
		scan.close();
		for(int i = 0; i < num; i++) {
			scan = new Scanner(new File("troop" + (i+1) + "data.txt"));
			troopSpeeds.add(scan.nextInt());
			troopHealth.add(scan.nextInt());
			scan.close();
			troopImage.add(ImageIO.read(new File("troop" + (i+1) + ".png")));
			troopsToAdd.add(0);
			System.out.println("Read in data for troop " + (i+1));
		}
	}

	public Troop troopFactory(int type) {
		Troop troop = new Troop();
		troop.speed = troopSpeeds.get(type);
		troop.health = troopHealth.get(type);
		troop.type = type;
		troop.width = troopImage.get(type).getWidth();
		troop.height = troopImage.get(type).getHeight();
		troop.ohealth = troop.health;
		return troop;
	}

	public class Troop {
		public int x;
		public int y;
		public int line;
		public int type;
		public int speed;
		public int health, ohealth;
		public int width, height;
		public int speedSpill = 0;
	}

	public class Tower {
		public int type;
		public boolean justFired = false;
		public int firedX, firedY;
		public int x;
		public int y;
		public int width, height;
		public Rectangle rect;
		public int fireRecharge = 0;
	}

	public static void main(String[] args) throws Exception {
		frame = new JFrame("John Game");
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.setContentPane(new StupidTest());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	public StupidTest() throws Exception {
		images = new BufferedImage[towers.length];
		for(int i = 0; i < towers.length; i++) {
			images[i] = ImageIO.read(new File((i+1) + ".png"));
		}
		frame.setIconImage(images[1]);
		grass = ImageIO.read(new File("grass.jpg"));
		cobblestone = ImageIO.read(new File("cobblestone.jpg"));
		readInWaveFile();
		readInTroopData();

		(new Thread(() -> {
			long gotime = 16;
			while(true) {
				try {
					Thread.sleep(16 - gotime < 0 ? 0 : 16 - gotime);
					long start = System.currentTimeMillis();
					loop();
					frameDue = true;
					repaint();
					long end = System.currentTimeMillis();
					gotime = start-end;
				} catch(Exception e) {e.printStackTrace();}
			}
		})).start();

		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent me) {
				if(mode.equals("Build map")) {
					if(areWeTouchingTheBlackButton(me.getX(), me.getY())) {
						mode = "Draw line";
						rectX = 0;
						rectY = 0;
						makingRect = false;
						rectctr = 0;
						hasMadeRects = true;
						return;
					}
					if(makingRect) {
						if(me.getX()-rectX <= 0 || me.getY() - rectY <= 0) {
							err("No road placed.");
							makingRect = false;
						} else {
							Rectangle rect = new Rectangle(rectX, rectY, me.getX()-rectX, me.getY()-rectY);
							System.out.println(rect);
							path[rectctr] = rect;
							rectctr++;
							makingRect = false;
							System.out.println("Drawn");
						}
					} else {
						rectX = me.getX();
						rectY = me.getY();
						makingRect = true;
						System.out.println("Making");
					}
				} else if(mode.equals("Draw line")) {
					if(areWeTouchingTheBlackButton(me.getX(), me.getY())) {
						mode = "Waiting for wave 1";
						hasDrawnWhite = true;
						makingRect = false;
						return;
					}
					if(makingRect) {
						Rectangle rect = new Rectangle(rectX, rectY, me.getX()-rectX, me.getY()-rectY);
						Rectangle newrect = null;
						if(Math.abs(rect.getWidth()) > Math.abs(rect.getHeight())) {
							newrect = new Rectangle(rectX, rectY, rect.width, 0);
							System.out.println("LONG");
						} else {
							newrect = new Rectangle(rectX, rectY, 0, rect.height);
							System.out.println("TALL");
						}
						System.out.println(newrect);
						lines[rectctr] = newrect;
						rectctr++;
						lineLength = rectctr;
						rectX = newrect.x + newrect.width;
						rectY = newrect.y + newrect.height;
						System.out.println("Drawn");
					} else {
						rectX = me.getX();
						rectY = me.getY();
						makingRect = true;
						System.out.println("Making");
					}
					
				} else if(mode.charAt(0) == 'W') {
					//waiting for a wave
					if(areWeTouchingTheBlackButton(me.getX(), me.getY())) {
						mode = "Round " + ++wave;
						inRound = true;
						roundTick = 0;
						roundWavesDone = 0;
						return;
					}
					if(selectedTower != -1) {
						Tower tower = new Tower();
						int mouseX = me.getX() - images[selectedTower].getWidth()/2;
						int mouseY = me.getY() - images[selectedTower].getHeight()/2;
						tower.x = mouseX;
						tower.y = mouseY;
						tower.type = selectedTower;
						tower.width = images[tower.type].getWidth();
						tower.height = images[tower.type].getHeight();
						boolean towerTouches = false;
						Rectangle rect = new Rectangle(tower.x, tower.y, tower.width, tower.height);
						tower.rect = rect;
						for(Tower tow : placedTowers) {
							if(tow.rect.intersects(rect)) {towerTouches = true; err("Tower touching other tower"); break; }
						}
						for(int i = 0; i < path.length; i++) {
							if(path[i] == null) break;
							if(path[i].intersects(rect)) {towerTouches = true; err("Tower touching path"); break;}
						}
						if(!towerTouches) {
							if(gold >= towerPrice[tower.type]) {
								placedTowers.add(tower);
								gold -= towerPrice[tower.type];
							} else {
								err("Tower costs too much!");
							}
						}
							
					}
					selectedTower = getClickedTowerIndex(me.getX(), me.getY());
				}
				//System.out.println(me.getX());
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent me) {
				mouseXg = me.getX();
				mouseYg = me.getY();
			}
		});
	}

	public void err(String msg) {
		errorMessage = msg;
		errorCounter = 200;
	}

	public boolean areWeTouchingTheBlackButton(int mouseX, int mouseY) {
		return mouseX > getWidth() - 50 && mouseY < 20;
	}

	public int getClickedTowerIndex(int mouseX, int mouseY) {
		int index = -1;
		
		for(int i = 0; i < towers.length; i++) {
			Rectangle rect = new Rectangle(i*64+32, getHeight()-bottomwidth+50, images[i].getWidth(), images[i].getHeight());
			if(rect.contains(new Point(mouseX, mouseY))) {
				index = i;
				return index;
			}
		}
		return index;
	}

	public class Wave {
		public ArrayList<Integer> times = new ArrayList<>();
		public ArrayList<Integer> quantities = new ArrayList<>();
		public ArrayList<Integer> troops = new ArrayList<>();
	}

	public void readInWaveFile() throws Exception {
		waves = new ArrayList<Wave>();
		Scanner scan = new Scanner(new File("waves.txt"));
		int waveIndex = 1;
		boolean waitingForWaveIndex = true;
		while(scan.hasNextLine()) {
			String waveLine = scan.nextLine();
			String[] spl = waveLine.split("WAVE ");
			int index = Integer.parseInt(spl[1]);
			if(index != waveIndex) {
				System.err.println("Wave index " + index + " not matching expected " + waveIndex);
			}
			String line;
			Wave wave = new Wave();
			while(scan.hasNextLine() && !(line = scan.nextLine()).trim().equals("")) {
				String[] parts =  line.split(": ");
				int time = Integer.parseInt(parts[0]);
				String[] parts2 = parts[1].split(" of ");
				int quantity = Integer.parseInt(parts2[0]);
				int troop = Integer.parseInt(parts2[1]);
				wave.times.add(time);
				wave.quantities.add(quantity);
				wave.troops.add(troop-1);
			}
			System.out.println("Processed wave " + waveIndex);
			waveIndex++;
			waves.add(wave);
		}
	}

	int roundTick = 0;
	int roundWavesDone = 0;
	int troopTickCounter = 0;
	public void loop() {
		if(dead) return;
		if(inRound) {
			if(waves.size() <= this.wave-1) {
				won = true;
				return;
			}
			Wave wave = waves.get(this.wave-1);
			for(int i = 0; i < wave.times.size(); i++) {
				if(roundTick == wave.times.get(i)) {
					troopsToAdd.set(i, troopsToAdd.get(i)+wave.quantities.get(i));
					roundWavesDone++;
				}
			}

			if(troopTickCounter == 0) {
				for(int i = 0; i < troopsToAdd.size(); i++) {
					if(troopsToAdd.get(i) > 0) {
						Troop troop = troopFactory(wave.troops.get(i));
						troop.x = lines[0].x - troop.width/2;
						troop.y = lines[0].y - troop.height/2;
						troopTickCounter = 35;
						troopsToAdd.set(i, troopsToAdd.get(i)-1);
						troops.add(troop);
					}
				}
			} else troopTickCounter--;
			if(troops.size() == 0 && roundWavesDone == wave.times.size()) {
				inRound = false;
				mode = "Waiting for wave " + (this.wave+1);
			}
			ArrayList<Troop> toRemove = new ArrayList<>();
			for(Tower tower : placedTowers) {
				if(tower.fireRecharge != 0) { tower.fireRecharge--; }
			}
			outer:
			for(Troop troop : troops) {
				int linenum = troop.line;
				Rectangle line = lines[linenum];
				for(Tower tower : placedTowers) {
					if(tower.fireRecharge != 0) { continue; }
					int xdist = tower.x - troop.x;
					int ydist = tower.y - troop.y;
					int distance = (int)Math.sqrt(xdist * xdist + ydist * ydist);
					if(distance <= towerRange[tower.type]) {
						troop.health -= towerDamage[tower.type];
						tower.fireRecharge = towerRecharge[tower.type];
						tower.justFired = true;
						tower.firedX = troop.x + troop.width/2;
						tower.firedY = troop.y + troop.height/2;
						if(troop.health <= 0) {
							gold += 1;
							toRemove.add(troop);
							continue outer;
						}
					}
				}
				if(linenum >= lineLength) {
					toRemove.add(troop);
					lives--;
					continue;
				}
				int ts = troop.speed/10;
				int tsp = troop.speed%10;
				tsp += troop.speedSpill;
				if(tsp >= 10) {
					ts += 1;
					tsp -= 10;
				}
				troop.speedSpill = tsp;
				if(line.width == 0) {
					if(line.height > 0) {
						troop.y += ts;
						if(troop.y >= line.y + line.height) {
							troop.line++;
						}
					} else {
						troop.y -= ts;
						if(troop.y <= line.y + line.height) {
							troop.line++;
						}
					}
				} else if(line.height == 0) {
					if(line.width > 0) {
						troop.x += ts;
						if(troop.x >= line.x + line.width) {
							troop.line++;
						}
					} else {
						troop.x -= ts;
						if(troop.x <= line.x + line.width) {
							troop.line++;
						}
					}
				}
			}
			for(Troop troop : toRemove) {
				troops.remove(troop);
			}
			if(lives <= 0) {
				dead = true;
			}
			roundTick++;
		}
	}

	int bottomwidth = 100;
	@Override
	public void paintComponent(Graphics gOld) {
		if(!frameDue) return;
		Graphics2D g = (Graphics2D)gOld;
		//g.setColor(Color.GREEN);
		int width = getWidth();
		int height = getHeight();
		if(dead) {
			g.setColor(Color.RED);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.WHITE);
			g.drawString("You dead!", 50, 50);
			return;
		}
		if(won) {
			g.setColor(Color.GREEN);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.WHITE);
			g.drawString("You win!", 50, 50);
			return;
		}
		g.setColor(Color.GREEN);

		for(int i = 0; i < width; i += grass.getWidth()) {
			for(int y = 0; y < height; y += grass.getHeight()) {
				g.drawImage(grass, i, y, null);
			}
		}
		//g.fillRect(0, 0, width, height-bottomwidth);

		g.setColor(new Color(200, 150, 100));
		for(int i = 0; i < path.length; i++) {
			if(path[i] == null) break;
			g.fillRect(path[i].x, path[i].y, path[i].width, path[i].height);
		}
		if(makingRect) {
			if(hasMadeRects) {
				g.setColor(Color.WHITE);
				g.drawLine(rectX, rectY, mouseXg, mouseYg);
			} else {
				g.fillRect(rectX, rectY, mouseXg-rectX, mouseYg-rectY);
			}
		}

		if(!hasDrawnWhite) {
			g.setColor(Color.WHITE);
			for(int i = 0; i < lines.length; i++) {
				if(lines[i] == null) break;
				g.drawLine(lines[i].x, lines[i].y, lines[i].x + lines[i].width, lines[i].y + lines[i].height);
			}
			g.drawString(makingRect ? "Click to place" : "Click to start", 5, 30);
		}
		g.setColor(new Color(255, 255, 60, 80));
		for(Tower tower : placedTowers) {
			//g.drawString("" + tower.fireRecharge, tower.x, tower.y);
			if(tower.justFired) {
				g.fillOval(tower.x-5, tower.y-5, tower.width+10, tower.height+10);
				g.drawLine(tower.x + tower.width/2, tower.y + tower.height/2, tower.firedX, tower.firedY);
				tower.justFired = false;
			}
			g.drawImage(images[tower.type], tower.x, tower.y, null);
		}
		g.setColor(Color.WHITE);
		for(Troop troop : troops) {
			g.drawImage(troopImage.get(troop.type), troop.x, troop.y, null);
			g.setColor(Color.RED);
			int mwidth = troopImage.get(troop.type).getWidth();
			g.fillRect(troop.x, troop.y, mwidth, 5);
			g.setColor(Color.GREEN);
			g.fillRect(troop.x, troop.y, (int)(mwidth * ((float)troop.health)/troop.ohealth), 5);
		}


		g.setColor(Color.GRAY);
		//g.fillRect(0, height-bottomwidth, width, bottomwidth);
		for(int i = 0; i < width; i += cobblestone.getWidth()) {
			g.drawImage(cobblestone, i, height-bottomwidth, null);
		}

		for(int i = 0; i < towers.length; i++) {
			g.setColor(Color.WHITE);
			if(i == selectedTower) {
				g.fillRect(i*64+32-5, height-bottomwidth+50-5, images[i].getWidth()+10, images[i].getHeight()+10);
			}
			if(mode.charAt(0) == 'W' && mouseXg > i*64+32 && mouseXg < i*64+32+images[i].getWidth() && mouseYg > height-bottomwidth+50 && mouseYg < height-bottomwidth+50+images[i].getHeight()) {
				g.drawString("Damage: " + towerDamage[i], width-100, height-bottomwidth+50);
				g.drawString("Cooldown: " + towerRecharge[i], width-100, height-bottomwidth+65);
				g.drawString("Range: " + towerRange[i], width-100, height-bottomwidth+80);
			}
			g.drawImage(images[i], i*64 + 32, height-bottomwidth + 50, null);
			g.drawString(towers[i], i*64 + 32, height-bottomwidth + 20);

			g.drawString("" + towerPrice[i], i*64+32+8, height-bottomwidth+50-13);
			g.setColor(Color.YELLOW);
			g.fillRect(i*64+32, height-bottomwidth+50-20, 5, 5);
		}



		g.setColor(Color.BLACK);
		g.fillRect(width - 50, 0, 50, 20);
		g.fillRect(0, height-bottomwidth-5, width, 5);
		g.setColor(Color.WHITE);
		g.drawString(mode, 5, 15);
		g.drawString("button", width-40, 15);
		g.drawString("Lives: " + lives, 5, 70);

		if(selectedTower >= 0) {
			g.setColor(new Color(255, 255, 255, 20));
			g.fillOval(mouseXg-towerRange[selectedTower], mouseYg-towerRange[selectedTower], towerRange[selectedTower]*2, towerRange[selectedTower]*2);
			g.drawImage(images[selectedTower], mouseXg - images[selectedTower].getWidth()/2, mouseYg - images[selectedTower].getHeight()/2, null);
		}

		if(hasDrawnWhite) {
			g.setColor(Color.YELLOW);
			g.fillRect(5, 25, 5, 5);
			g.setColor(Color.WHITE);
			g.drawString("Gold: " + gold, 12, 30);
		}

		if(errorCounter > 0) {
			errorCounter--;
			g.setColor(Color.RED);
			g.fillRect(5, 45, 5, 5);
			g.setColor(Color.WHITE);
			g.drawString(errorMessage, 12, 50);
		}
	}
}
