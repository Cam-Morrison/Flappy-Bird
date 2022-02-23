package code;


import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import java.awt.*;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. By default GameCore
// will handle the 'Escape' key to quit the game but you should
// override this with your own event handler.

/**
 * @author David Cairns
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
    //boolean paused = false;
    private static boolean fullScreen = false; //Checks if application full-screen across all windows
    
    // Game resources
    Animation landing;
    
    Sprite	player = null;
    ArrayList<Sprite> clouds = new ArrayList<Sprite>();

    TileMap tmap = new TileMap();	// Our tile map, note that we load it in init()
    
    long total;         			// The score will be the total time elapsed since a crash
    
    
    //Parallax images taken from free licensing publisher https://digitalmoons.itch.io/free-parallax-desert-background-seamless
    private Image bgImage1, bgImage2, bgImage3, bgImage4; //background images
    //Used to move background at different speeds to create realistic illusion 
    private int bg1location = 0;
    private int bg2location;
    private int fg1location = 0;
    private int fg2location;
    
    private int xo = 0;
    private int yo = 0;
    
    
   
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

      	//Parallax vector images from https://raventale.itch.io/parallax-background
		try {
			//Width + 3 to avoid cutting when repeating image, loaded in here to reduce lag in draw method
			bgImage1 = ImageIO.read(new File("src/images/Sky.png"));
			bgImage1 = bgImage1.getScaledInstance(screenWidth + 3, screenHeight, Image.SCALE_FAST);
			System.out.println(getWidth());
			bgImage2 = ImageIO.read(new File("src/images/Moon.png"));
			bgImage2 = bgImage2.getScaledInstance(screenWidth + 3, screenHeight, Image.SCALE_FAST);
			bgImage3 = ImageIO.read(new File("src/images/Mountains.png"));
			bgImage3 = bgImage3.getScaledInstance(screenWidth + 3, screenHeight, Image.SCALE_FAST);
			bgImage4 = ImageIO.read(new File("src/images/Desert.png"));
			bgImage4 = bgImage4.getScaledInstance(screenWidth + 3, screenHeight, Image.SCALE_FAST);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        // Create a set of background sprites that we can 
        // rearrange to give the illusion of motion

        landing = new Animation();
        landing.loadAnimationFromSheet("src/images/landbird.png", 4, 1, 60);
        
        // Initialise the player with an animation
        player = new Sprite(landing);
        
        // Load a single cloud animation
        Animation ca = new Animation();
        ca.addFrame(loadImage("src/images/cloud.png"), 1000);
        
        // Create 3 clouds at random positions off the screen
        // to the right
        for (int c=0; c<3; c++)
        {
        	s = new Sprite(ca);
        	s.setX(screenWidth + (int)(Math.random()*200.0f));
        	s.setY(30 + (int)(Math.random()*150.0f));
        	s.setVelocityX(-0.02f);
        	s.show();
        	clouds.add(s);
        }

        initialiseGame();
        System.out.println(tmap);
        
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
    	total = 0;
    	      
        player.setX(64);
        player.setY(200);
        player.setVelocityX(0);
        player.setVelocityY(0);
        player.show();
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
        
        bg1location--; bg2location--;
        fg1location-=3; fg2location-=3;
        
        if(bg1location<-screenWidth) bg1location = screenWidth;
        if(bg2location<-screenWidth) bg2location = screenWidth;
        if(fg1location<-screenWidth) fg1location = screenWidth;
        if(fg2location<-screenWidth) fg2location = screenWidth;
        
        // Apply offsets to sprites then draw them
        for (Sprite s: clouds)
        {
        	s.setOffsets(0,yo);
        	s.draw(g);
        }

        // Apply offsets to player and draw 
        player.setOffsets(0, yo);
        player.draw(g);
        player.drawBoundingCircle(g);
                
        // Apply offsets to tile map and draw  it, move background left
        tmap.draw(g,xo,yo);    
        
        xo--;
        
        // Show score and status information
        String msg = String.format("Score: %d", total/100);
        g.setColor(Color.white);
        g.drawString(msg, getWidth() - 80, 50);
        
        
    }

    /**
     * Update any sprites and check for collisions
     * 
     * @param elapsed The elapsed time between this call and the previous call of elapsed
     */    
    public void update(long elapsed)
    {
        // Make adjustments to the speed of the sprite due to gravity
        player.setVelocityY(player.getVelocityY()+(gravity*elapsed));   	
       	player.setAnimationSpeed(1.0f);
       	
       	if (flap) 
       	{
       		player.setAnimationSpeed(1.8f);
       		player.setVelocityY(-0.075f);
       	}
                
       	for (Sprite s: clouds)
       		s.update(elapsed);
       	
        // Now update the sprites animation and position
        player.update(elapsed);
       
        // Then check for any collisions that may have occurred
        handleScreenEdge(player, tmap, elapsed);
        checkTileCollision(player, tmap);
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
    	
    	if (key == KeyEvent.VK_UP) flap = true;
    	if (key == KeyEvent.VK_F) { //Toggle fullscreen
    		dispose();
    		Game gm = new Game();
	    	Thread th = new Thread(new Runnable() {
	    		public void run(){
	    			if(fullScreen == false) {
	    				System.out.println(fullScreen);
	    				fullScreen = true;
	    		        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
	    				screenWidth = (int) size.getWidth();
	    				screenHeight = (int) size.getHeight();
	    			} else {
	    				System.out.println(fullScreen);
	    				fullScreen = false;
	        			screenWidth = 512; screenHeight = 384;
	        		} 
    				gm.init();
    				gm.run(fullScreen, screenWidth, screenHeight);
	    		}});
	    		th.start();
    	}
    	
    	if (key == KeyEvent.VK_S)
    	{
    		// Example of playing a sound as a thread
    		Sound s = new Sound("src/sounds/caw.wav");
    		s.start();
    	}
    }

    public boolean boundingBoxCollision(Sprite s1, Sprite s2)
    {
    	return false;   	
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
    	// Take a note of a sprite's current position
    	float sx = s.getX();
    	float sy = s.getY();
    	
    	// Find out how wide and how tall a tile is
    	float tileWidth = tmap.getTileWidth();
    	float tileHeight = tmap.getTileHeight();
    	
    	// Divide the sprite’s x coordinate by the width of a tile, to get
    	// the number of tiles across the x axis that the sprite is positioned at 
    	int	xtile = (int)(sx / tileWidth);
    	// The same applies to the y coordinate
    	int ytile = (int)(sy / tileHeight);
    	
    	// What tile character is at the top left of the sprite s?
    	char ch = tmap.getTileChar(xtile, ytile);
    	
    	
    	if (ch != '.') // If it's not a dot (empty space), handle it
    	{
    		// Here we just stop the sprite. 
    		s.stop();
    		while(ch != '.') {
    			
    			s.setY(sx--);
    		}
    		// You should move the sprite to a position that is not colliding
    	}
    	
    	// We need to consider the other corners of the sprite
    	// The above looked at the top left position, let's look at the bottom left.
    	xtile = (int)(sx / tileWidth);
    	ytile = (int)((sy + s.getHeight())/ tileHeight);
    	ch = tmap.getTileChar(xtile, ytile);
    	
    	// If it's not empty space
     	if (ch != '.') 
    	{
    		// Let's make the sprite bounce
    		s.setVelocityY(-s.getVelocityY()); // Reverse velocity 
    	}
    }


	public void keyReleased(KeyEvent e) { 

		int key = e.getKeyCode();

		// Switch statement instead of lots of ifs...
		// Need to use break to prevent fall through.
		switch (key)
		{
			case KeyEvent.VK_ESCAPE : stop(); break;
			case KeyEvent.VK_UP     : flap = false; break;
			default :  break;
		}
	}

}
