package code;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. By default GameCore
// will handle the 'Escape' key to quit the game but you should
// override this with your own event handler.

/**
 * @author Cameron Morrison
 *
 */
@SuppressWarnings("serial")

public class Game extends GameCore
{
	// Useful game constants
	static int screenWidth = 512; 
	static int screenHeight = 384;

    float 	lift = 0.05f;
    float	gravity = 0.0001f;
    
    // Game state flags
    private boolean flap = false;
    private boolean pause = true;
    private boolean debugMode = true;
    private boolean checkCollision;
    
    // Game resources
    Animation bird;
    Animation coin;
    Animation rockAnim;
    
    Sprite	player = null;
    ArrayList<Sprite> rocks = new ArrayList<Sprite>();

    TileMap tmap = new TileMap();	// Our tile map, note that we load it in init()
    
    long total = 0;  // The score will be the total time elapsed since a crash
       
    //Parallax images taken from free licensing publisher https://digitalmoons.itch.io/free-parallax-desert-background-seamless
    private Image bgImage1, bgImage2, bgImage3, bgImage4; //background images
    private Image playBtn;
    //Used to move background at different speeds to create realistic illusion 
    private int bg1location = 0;
    private int bg2location;
    private int fg1location = 0;
    private int fg2location;
    
    private double rotation = 90;
    private double scale = 1;
   
    
    
    private static int offsetMapX;
   
    /**
	 * The obligatory main method that creates
     * an instance of our class and starts it running
     * 
     * @param args	The list of parameters this program might use (ignored)
     */
    public static void main(String[] args) {

        Game gct = new Game();
        gct.init();
        gct.run(false,screenWidth,screenHeight);   
      
    } 

    /**
     * Initialise the class, e.g. set up variables, load images,
     * create animations, register event handlers
     */
    public void init()
    {         
        Sprite s;	// Temporary reference to a sprite

        // Load the tile map and print it out so we can check it is valid
        tmap.loadMap("src/maps", "map.txt");
        
        setSize(tmap.getPixelWidth()/4, tmap.getPixelHeight());
        setVisible(true);
        setResizable(false);
        setLocationRelativeTo(null);

      	//Parallax vector images from https://raventale.itch.io/parallax-background
		try {
			//Width + 3 to avoid cutting when repeating image, loaded in here to reduce lag in draw method
			bgImage1 = ImageIO.read(new File("src/images/Sky.png"));
			bgImage1 = bgImage1.getScaledInstance(screenWidth + 3, screenHeight, Image.SCALE_FAST);
			bgImage2 = ImageIO.read(new File("src/images/Moon.png"));
			bgImage2 = bgImage2.getScaledInstance(screenWidth + 3, screenHeight, Image.SCALE_FAST);
			bgImage3 = ImageIO.read(new File("src/images/Mountains.png"));
			bgImage3 = bgImage3.getScaledInstance(screenWidth + 3, screenHeight, Image.SCALE_FAST);
			bgImage4 = ImageIO.read(new File("src/images/Desert.png"));
			bgImage4 = bgImage4.getScaledInstance(screenWidth + 3, screenHeight, Image.SCALE_FAST);
			playBtn = ImageIO.read(new File("src/images/PlayButton.png"));
			playBtn = playBtn.getScaledInstance(screenWidth/5, screenHeight/8, Image.SCALE_SMOOTH);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        // Create a set of background sprites that we can 
        // rearrange to give the illusion of motion

        bird = new Animation();
        bird.loadAnimationFromSheet("src/images/landbird.png", 4, 1, 60);
     
        // Initialise the player with an animation
        player = new Sprite(bird);
        
        rockAnim = new Animation();
        rockAnim.addFrame(loadImage("src/images/rock.png"), 1000);
        
        // Create 3 rocks at random positions off the screen to the right
        for (int i = 0; i < 3; i++)
        {
        	s = new Sprite(rockAnim);
        	rocks.add(s);
        }

        initialiseGame();
       
        bg2location = screenWidth;
        fg2location = screenWidth;   
    }

    /**
     * You will probably want to put code to restart a game in
     * a separate method so that you can call it to restart
     * the game.
     */
    public void initialiseGame()
    {
    	offsetMapX = 200;
        player.setX(64);
        player.setY(200);
        player.setVelocityX(0);
        player.setVelocityY(0);
        player.show();
        checkCollision = true;
        for(Sprite s : rocks) {
        	s.show();
        	deployAsteroid(s);
        }
    }
    
    
    /**
     * Draw the current state of the game
     */
    public void draw(Graphics2D g)
    {    	
    	// Be careful about the order in which you draw objects - you
    	// should draw the background first, then work your way 'forward'

    	
    	// First work out how much we need to shift the view 
    	// in order to see where the player is.

        // If relative, adjust the offset so that
        // it is relative to the player

        g.setColor(Color.white);
        g.fillRect(0, 0, screenWidth, screenHeight);

        
        //Parallax background
        g.drawImage(bgImage1, 0, 0, null); 
        g.drawImage(bgImage2, 0, 0, null);
        
        g.drawImage(bgImage3, bg1location, 0, null);
        g.drawImage(bgImage3, bg2location, 0, null);
        
        g.drawImage(bgImage4, fg1location, 0, null);
        g.drawImage(bgImage4, fg2location, 0, null);
         
        for (Sprite s: rocks){
	    	s.setRotation(rotation);
	    	s.drawTransformed(g);
	    	
        	if(s.getX() < -50) {
        		deployAsteroid(s);
        	}
        }
        
        player.draw(g);
           
        // Apply offsets to tile map and draw  it, move background left
        tmap.draw(g,offsetMapX,0); 
        
        if(pause == false) {
        	offsetMapX--;
        	
    	    bg1location--; bg2location--;
    	    fg1location-=3; fg2location-=3;
            
            if(bg1location<-screenWidth) bg1location = screenWidth;
            if(bg2location<-screenWidth) bg2location = screenWidth;
            if(fg1location<-screenWidth) fg1location = screenWidth;
            if(fg2location<-screenWidth) fg2location = screenWidth;
        }
          
        // Show score and status information
        String msg = String.format("Score: %d", total/100);
        g.setFont(new Font("Verdana", Font.BOLD, 18)); 
        g.setColor(Color.WHITE);
        g.drawString(msg, screenWidth - 120, 50);
        

        if(debugMode) {
        	player.drawBoundingCircle(g);
	        String debug = "FPS: " + (int)getFPS();
	        g.setColor(Color.white);
	        g.drawString(debug, 40, 50);
	        for(Sprite s: rocks) {
	        	s.drawBoundingCircle(g);
	        	g.drawString("X:"+(int)s.getX(), s.getX(), s.getY());
	        }
	        g.drawString("Y:"+(int)player.getY(), player.getX(), player.getY());
        }  
        
    	if(pause == true) {
    		g.drawImage(playBtn, 200, 200, null); 
    	}
    }

    private void deployAsteroid(Sprite s) {
    	s.setX(screenWidth + (int)(Math.random()*200.0f));
    	s.setY((int)Math.floor(Math.random()*(screenHeight-s.getHeight())+0));
    	s.setVelocityX(-0.1f);
	}

	/**
     * Update any sprite's and check for collisions
     * 
     * @param elapsed The elapsed time between this call and the previous call of elapsed
     */    
    public void update(long elapsed)
    {
    	player.setAnimationSpeed(1.0f);
        // Now update the sprite's animation and position
        player.update(elapsed);
        
         
        
    	if(pause == false) {	
    		//Increase score
    		total++;
	        // Make adjustments to the speed of the sprite due to gravity
	        player.setVelocityY(player.getVelocityY()+(gravity*elapsed));   	
	       
	       	if (flap) 
	       	{
	       		player.setAnimationSpeed(1.8f);
	       		player.setVelocityY(-0.075f);
	       	}
	                
	       	for (Sprite s: rocks) {
	       		s.update(elapsed);
	       		if(boundingBoxCollision(player, s)) {
	       			if(checkCollision) {
		       			handleCollison(player);
		       			s.hide();
		       			deployAsteroid(s);
	       			}
	       		}
	       	}
	       	rotation++;
	       	
	        // Then check for any collisions that may have occurred
	        handleScreenEdge(player, tmap, elapsed);
	        checkTileCollision(player, tmap);    
    	}
    }
    
    
    /**
     * Checks and handles collisions with the edge of the screen
     * 
     * @param s			The Sprite to check collisions for
     * @param tmap		The tile map to check 
     * @param elapsed	How much time has gone by since the last call
     */
    public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed)
    {
    	// This method just checks if the sprite has gone off the bottom screen.
    	// Ideally you should use tile collision instead of this approach
    	
    	//If player is too low
        if (s.getY() + s.getHeight() > tmap.getPixelHeight())
        {
        	// Put the player back on the map 1 pixel above the bottom
        	s.setY(tmap.getPixelHeight() - s.getHeight() - 1); 
        	
        	// and make them bounce
        	s.setVelocityY(-s.getVelocityY());
        }
        //If player is too high
        if(s.getY() - s.getHeight() < 0) {
        	//don't let player go above map
        	s.setY(0 + s.getHeight() + 1); 
        }
    }
    
    
     
    /**
     * Override of the keyPressed event defined in GameCore to catch our
     * own events
     * 
     *  @param e The event that has been generated
     */
    public void keyPressed(KeyEvent e) 
    { 
    	int key = e.getKeyCode();
    	
    	if (key == KeyEvent.VK_ESCAPE) stop();
    	
    	if (key == KeyEvent.VK_UP) {
    		flap = true; 
    	}  	
    	if(key == KeyEvent.VK_SPACE)
    	{
    		pause = false;
    	}
    }
    
    //Makes a bird noise
    public void caw() {
		Sound s = new Sound("src/sounds/caw.wav");
		s.start();
    }
    
    public boolean boundingBoxCollision(Sprite s1, Sprite s2)
    {
    	 int dx,dy,minimum;   
    	   
    	  dx = (int) (s1.getX() - s2.getX()); 
    	  dy = (int) (s1.getY() - s2.getY()); 
    	  minimum = (int) (s1.getRadius() + s2.getRadius()); 
    	    
    	  return (((dx * dx) + (dy * dy)) < (minimum * minimum));
    }
    
    /**
     * Check and handles collisions with a tile map for the
     * given sprite 's'. Initial functionality is limited...
     * 
     * @param s			The Sprite to check collisions for
     * @param tmap		The tile map to check 
     */

	public void checkTileCollision(Sprite s, TileMap tmap)
    {    	
    	float sx = s.getX() + s.getRadius() - offsetMapX;
    	float sy = s.getY() + s.getRadius();  
    	
    	char ch;
    	int xtile, ytile;
    	double x, y;
    	for(int angle = 0; angle < 6; angle++) {
    		x = sx + (s.getRadius() * Math.cos(Math.toRadians(angle * 60)));
    		y = sy + (s.getRadius() * Math.sin(Math.toRadians(angle * 60)));
    		// Find out how wide and how tall a tile is
        	xtile = (int)(x /  tmap.getTileWidth());
        	ytile = (int)(y / tmap.getTileHeight());
    		ch = tmap.getTileChar(xtile, ytile);
    		if(ch == '.') continue;	
        	if (ch == 'p' || ch == 'b' || ch == 't') {
        		handleCollison(s);
    	        break;
    	    } 
        	if(ch == '?'){
				if(offsetMapX > -500) {
					continue;
				}
				checkCollision = false;
	    		s.setVelocityY(0);
	    		s.setVelocityX(0.3f);
	            TimerTask timerTask = new TimerTask() {
	                @Override
	                public void run() {
	                	//Start new level once animation done or exit
	                	changeLevel();
	                	initialiseGame();
	                	pause = false;
	                	cancel();
	                	
	                }
	            };
	            Timer timer = new Timer("MyTimer");
	            //After two seconds, execute timer function
	            timer.scheduleAtFixedRate(timerTask, 2000, 1000);
	            pause = true;
				break;
    	    }
        	
        }
    }
    	

    //If collision happens
    private void handleCollison(Sprite s) {	
		caw();
		pause = true;
		total = 0;
		for(Sprite rock : rocks)
			deployAsteroid(rock);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
            	//unpauses sprite
            	pause = false;
            }
        };
        Timer timer = new Timer("MyTimer");
        //After one seconds, execute timer function
        timer.scheduleAtFixedRate(timerTask, 1000, 30000);
		s.stop();
		offsetMapX = offsetMapX + s.getWidth() * 3;
		s.setY(screenWidth/2 - s.getHeight());
    }


	public void keyReleased(KeyEvent e) { 

		int key = e.getKeyCode();

		// Switch statement instead of lots of ifs...
		// Need to use break to prevent fall through.
		switch (key)
		{
			case KeyEvent.VK_ESCAPE : stop(); break;
			case KeyEvent.VK_UP     : flap = false; break;
			case KeyEvent.VK_1 		: debugMode = !debugMode; System.out.println(debugMode); break;
			case KeyEvent.VK_2 		: offsetMapX = -1650; break;
			default :  break;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(pause == true) pause = false;
	}
	
	public void changeLevel() {
		tmap.loadMap("src/maps", "map2.txt");
	}
}
