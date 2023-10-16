import java.io.File;
import java.util.*;
import processing.core.*;
 
public class Pathfinder extends PApplet {
  
  // used for Serializable interface inherited via PApplet
  private static final long serialVersionUID = 1L;
  
  // ====================== //
  // ==== GENERAL CODE ==== //    (shared between modes)
  // ====================== //
  
  // WallSets are primary containers for the "map" that's edited
  //   and played on
  // wsCurr is always updated to refer to the current WallSet
  ArrayList<WallSet> wsList = new ArrayList<>();
  int wsIndex = 0;
  WallSet wsCurr;
  
  // Points primarily represent ends of walls but can also
  //   represent the mouse, moving objects in a game, etc
  Point mouse = new Point(0,0);
  
  // mode control variables
  Mode[] modes = new Mode[]{new BuildMode(), new PlayMode()};
  final int BUILD_MODE = 0;
  final int PLAY_MODE = 1;
  int modeIndex = BUILD_MODE;
  
  // modifier keys tracked here
  boolean ctrlHold, shiftHold;
  
  public void setup() {
    size(1000, 800);
    colorMode(HSB, 360, 100, 100, 100);
    
    loadWallSets();
    wsCurr = wsList.get(0);
  }
  
  // loads all valid wallsets in folder
  void loadWallSets() {
    File folder = new File("wallsets/");
    
    // check valid folder structure
    if (folder.exists() && folder.isDirectory()) {
      File[] files = folder.listFiles();
      if (files.length == 0) {
        System.out.println("No files in wallsets folder.");
      }
      
      System.out.println("Loading from wallsets folder:");
      for (File wsFile : folder.listFiles()) {
        if (!wsFile.isDirectory()) {
          WallSet ws = WallSet.fromFile(wsFile);
          if (ws != null) wsList.add(ws);
        }
      }
      if (wsList.isEmpty()) {
        System.out.println("No valid files in wallsets folder.");
      }
    }
    else {
      System.err.println("Couldn't find wallsets folder! Should be at " +
                         folder.getAbsolutePath());
    }
    
    if (wsList.isEmpty()) wsList.add(new WallSet());
  }
  
  public void draw() {
    // keep mouse Point updated
    mouse.x = mouseX;
    mouse.y = mouseY;
 
    // draw based on mode
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].draw();
    }
    else {
      background(0); // black
      fill(0, 10, 100); // 10% red
      textAlign(CENTER, CENTER);
      textSize(48);
      text("Invalid mode!", width/2, height/2);
    }
  }
  
  public void keyPressed() {
    if (keyCode == CONTROL) ctrlHold = true;
    if (keyCode == SHIFT) shiftHold = true;
    
    // most things should only work on a valid mode
    if (0 <= modeIndex && modeIndex < modes.length) {
      // space will switch modes, so clean up the old one
      if (key == ' ') {
        modes[modeIndex].cleanup();
      }
      // send the key press to the mode
      else {
        modes[modeIndex].keyPressed();
      }
    }
    // space cycles to new mode and starts it up
    if (key == ' ') {
      modeIndex = (modeIndex + 1) % modes.length;
      modes[modeIndex].init();
    }
  }
  
  // most controls just get passed to the correct mode
  public void keyReleased() {
    if (keyCode == CONTROL) ctrlHold = false;
    if (keyCode == SHIFT) shiftHold = false;
    
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].keyReleased();
    }
  }
  
  public void mousePressed() {
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].mousePressed();
    }
  }
  
  public void mouseDragged() {
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].mouseDragged();
    }
  }
  
  public void mouseReleased() {
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].mouseReleased();
    }
  }
  
  // interface for different modes with different behaviors
  interface Mode {
    void draw();
    
    // default: allows interfaces to provide simple implementations,
    //   allowing subclasses to treat those methods as optional
    default void init() {}
    default void cleanup() {}
    
    default void keyPressed() {}
    default void keyReleased() {}
    
    default void mousePressed() {}
    default void mouseDragged() {}
    default void mouseReleased() {}
  }

  // ==================== //
  // ==== BUILD MODE ==== //
  // ==================== //
  
  // this mode allows display, editing, loading, saving, etc,
  //   of WallSets
  class BuildMode implements Mode {
    Point lastPoint = null;       // last point created/dragged
    Point lastPointOrigin = null; // point where dragging began
    
    String typed = null;     // used for typed input
    boolean saving = false;  // sub-mode for typing save filename
    
    public void draw() {
      background(120, 50, 100);  // green
      
      // NOTE: this class is BuildMode, so "this" would refer to the
      //     instance of BuildMode
      //   "Pathfinder.this" is a "qualified this" allowing access to the
      //     instance of the Pathfinder / PApplet object representing
      //     representing the program
      //   (as an inner class, members of BuildMode are part of a
      //     BuildMode instance AND part of a Pathfinder instance)
      wsCurr.display(Pathfinder.this);
      
      // draw last point and line connecting it to mouse
      if (lastPoint != null) {
        drawCrossingLine(lastPoint, mouse);
        lastPoint.display(Pathfinder.this);
      }
      
      showInstructions();
      
      if (saving) showSavePrompt();
    }
    
    void showSavePrompt() {
      fill(100, 0, 0);
      textAlign(CENTER, BOTTOM);
      textSize(24);
      float tSize = textAscent() + textDescent();
      
      text("Enter file name:", width/2, height - tSize - 5);
      text(typed, width/2, height - 5);
    }
    
    void showInstructions() {
      fill(0, 40);  // black, 40% opacity
      textAlign(LEFT, BOTTOM);
      textSize(12);
      float tSize = textAscent() + textDescent();
      float y = 0;
      
      String label = "BUILD MODE - #" + wsIndex + " - " + wsCurr.getName();
      text(label,                                      5, y += tSize);
      text("space: switch mode (works in both modes)", 5, y += tSize);
      
      y += tSize;
      int maxWall = Math.min(9, wsList.size()-1);
      text("any #: load wallset (0-" + maxWall + ")", 5, y += tSize);
      
      // unicode 2191 & 2193: up & down arrows
      text("\u2191\u2193: browse wall sets",    5, y += tSize);
      text("n: new wall set",                   5, y+= tSize);
      text("s: save current walls",             5, y += tSize);
      text("r: revert to saved walls",          5, y += tSize);
      
      y += tSize;
      text("click: create endpoint",            5, y += tSize);
      text("right-click endpoint: remove wall", 5, y += tSize);
      text("click and drag: move endpoint",     5, y += tSize);
      text("w: Add random wall.",               5, y += tSize);
      
      y += tSize;
      text("ctrl-z: " + wsCurr.undoPeek(), 5, y += tSize);
      text("shift-ctrl-z: " + wsCurr.redoPeek(), 5, y += tSize);
    }
    
    // draws line from a->b, with intersections with walls
    void drawCrossingLine(Point a, Point b) {
      List<Point> intersects = wsCurr.intersections(a, b);
   
      strokeWeight(3);
      if (intersects.isEmpty()) {
        stroke(120, 100, 50); // green - no crossings
        line(a.x, a.y, b.x, b.y);
      }
      else {
        stroke(0, 100, 100); // red - crosses other lines
        line(a.x, a.y, b.x, b.y);
        
        // display each intersection point
        for (Point intersect : intersects) {
          intersect.display(Pathfinder.this);
        }
      }
    }
    
    public void keyPressed() {
      // typing a file name as part of save operation
      if (saving) {
        // typable ASCII values get typed
        if (key >= 32 && key < 127) {
          typed += key;
        }
        // backspace backspaces
        if (key == BACKSPACE && typed.length() > 0) {
          typed = typed.substring(0, typed.length()-1);
        }
        // enter completes save operation
        if (key == ENTER) {
          finishSave();
        }
      }
      // normal controls when not in mid-save
      else {
        if (key == 'w') wsCurr.add(new Wall(Pathfinder.this));
        if (key == 'r') wsCurr.revert();
        if ('0' <= key && key <= '9') loadWalls(key - '0');
        if (key == 's') {
          // begin typing name to save to, if none exists
          if (wsCurr.name == null) {
            saving = true;
            typed = "";
          }
          // or just save
          else {
            wsCurr.save();
          }
        }
        // ctrl-Z with or without shift for undo/redo
        // checks keyCode not key because weird stuff happens when
        //   holding control
        if ((keyCode == 'z' || keyCode == 'Z') && ctrlHold) {
          if (shiftHold) wsCurr.redo();
          else           wsCurr.undo();
        }
        // secret support for ctrl-Y
        if (keyCode == 'y' && ctrlHold) wsCurr.redo();
        if (keyCode == UP) {
          loadWalls( (wsIndex + 1) % wsList.size() );
        }
        if (keyCode == DOWN) {
          loadWalls( (wsIndex + wsList.size() - 1) % wsList.size());
        }
        if (key == 'n') {
          lastPoint = null;  // deselect point
          
          wsCurr = new WallSet(); // brand new WallSet at end of list
          wsIndex = wsList.size();
          wsList.add(wsCurr);
        }
      }
    }
    
    // attempts to name and save file based on typed name
    void finishSave() {
      // entering nothing cancels save
      if (typed.isEmpty()) return;
      
      // add file extension if none was provided
      if (!typed.contains(".")) typed += ".walls";
      
      // lock in name
      wsCurr.name = typed;
      
      // try to save, but don't keep name if it fails
      //   (it's probably an invalid name if that happens)
      if (!wsCurr.save()) wsCurr.name = null;
      
      // reset typing vars and exit saving operation
      typed = null;
      saving = false;
    }
    
    // load a set of walls by number
    void loadWalls(int index) {
      if (index >= wsList.size()) {
        System.err.println("Only " + wsList.size() + " wall sets.");
      }
      else {
        lastPoint = null;  // deselect points when loading
        
        wsIndex = index;
        wsCurr = wsList.get(index);
      }
    }
    
    public void cleanup() {
      lastPoint = null;  // deselect
    }
    
    public void mousePressed() {
      Point clicked = findPoint(mouseX, mouseY);
      if (mouseButton == LEFT) {
        // create new Point if one was not already there
        if (clicked == null) {
          clicked = new Point(mouseX, mouseY);
          
          // create new wall if there was another point before
          if (lastPoint != null) {
            wsCurr.add(new Wall(clicked, lastPoint));
          }
          // ... or just start forming a wall at that point
          else {
            lastPoint = clicked;
          }
        }
        // start moving point if there was already a point there
        else {
          lastPointOrigin = new Point(clicked);
        }
        
        // whether point is new or pre-existing, select it for
        //   potential dragging/moving
        lastPoint = clicked;
      }
      else if (mouseButton == RIGHT) {
        // remove Point or Wall
        if (clicked != null) {
          if (clicked.wall == null) {
            if (clicked == lastPoint) lastPoint = null;
          }
          else {
            wsCurr.rem(clicked.wall);
            // reset lastPoint if part of this wall
            if (lastPoint != null && lastPoint.wall == clicked.wall) {
              lastPoint = null;
            }
          }
        }
        // clicked on nothing; deselect lastPoint
        else if (lastPoint != null) lastPoint = null;
      }
    }
    
    public void mouseDragged() {
      // drag point if one is selected
      if (lastPoint != null) {
        lastPoint.x = mouseX;
        lastPoint.y = mouseY;
      }
    }
    
    public void mouseReleased() {
      // "let go" of any point that's already part of a wall, after
      //   dragging or placing it
      if (lastPoint != null && lastPoint.wall != null) {
        // also lock in movement if it was moved
        if (lastPointOrigin != null && !lastPoint.equals(lastPointOrigin)) {
          wsCurr.finishMove(lastPoint, lastPointOrigin);
          lastPointOrigin = null;
        }
        lastPoint = null;
      }
    }
    
    // finds Point with 5 px of a given point
    Point findPoint(float x, float y) {
      if (lastPoint != null &&
          dist(lastPoint.x, lastPoint.y, x, y) <= 5) {
        return lastPoint;
      }
      
      for (Point p : wsCurr.points) {
        if (dist(p.x, p.y, x, y) <= 5) return p;
      }
      
      return null;
    }
  }
  
  // ===================
  // ==== PLAY MODE ====
  // ===================
  class PlayMode implements Mode {
    boolean playPaused = false;
    
    Player player;
    Mover[] ghosts;
    
    int start;
    int started = 0;
    
    // TODO: graph settings?
    boolean DISPLAY_POSSIBLE_PATHS = false; // displays all connections between possible movement points (movers, players, walls)
    boolean DISPLAY_MOVEMENTS = false; // displays the paths that ghosts will be taking to get to the player
    
    // controls display and movement of game
    public void draw() {
      // TODO: update movement graph based on Mover positions
      
      background(0, 0, 100);  // white
   
      if (!playPaused) {
    	  player.move();
    	  if(frameCount % 5 == 0 || started == 0) {
        	  updateWallConnections();
        	  updateGhostConnections();
        	  started = 1;
    	  }
      }
      
      // half the time display player first; half the time last
      if (frameCount % 2 != 0) player.display(Pathfinder.this);
      int elapsed = millis() - start;
      // displayConnections(); // shows connections between walls
      // ghostConnections(); // shows immediate connections so....
      // displayGhostDirections(); // shows paths yaknow
      
   // handle all ghosts
      for (Mover m : ghosts) {
        if (!playPaused) m.move();
        m.display(Pathfinder.this);
        
        // TODO: display movement paths?
        if(DISPLAY_MOVEMENTS) {
        	displayGhostDirections(); 
        	// doesn't rly work rn 
        	// im kinda lazy
        }
      }
      
      // half the time display player last; half the time first
      if (frameCount % 2 == 0) player.display(Pathfinder.this);
      
      wsCurr.display(Pathfinder.this);
      
      
      if (elapsed < 10000) {
        // in 1st 10 seconds of play mode, display instructions
        //   fading from 40 to 0 opacity in seconds 5-10,
        //   also keeping it no more than 40 in seconds 0-5
        fill(0, min(40, map(elapsed, 5000, 10000, 40, 0)));
        showInstructions();
      }
      
      // TODO: display graph (perhaps based on settings)
    }
    
    void showInstructions() {
      textAlign(LEFT, BOTTOM);
      textSize(12);
      float tSize = textAscent() + textDescent();
      float y = 0;
      
      text("PLAY MODE",          5, y += tSize);
      text("space: switch mode", 5, y += tSize);
      text("p: pause/unpause",   5, y += tSize);
      text("r: reset game",      5, y += tSize);
    }
    
    public void keyPressed() {
      if (key == 'p') playPaused = !playPaused;
      if (key == 'r') resetPlayers();
      
      // TODO: control graph settings?
      
    }
    
    // entering and exiting play mode
    public void init() {
      resetPlayers();
      
      // TODO: create graph among Points
    	for(Point compareToPoint: wsCurr.points) {
    		compareToPoint.connections = new HashSet<>();
    		for(Point curPoint: wsCurr.points) {
	    		if(wsCurr.isClearPath(curPoint, compareToPoint) && compareToPoint != curPoint) {
	    			 compareToPoint.connections.add(curPoint);
	    	    }
    		}
    	}
    }
    
    
    void updateGhostConnections() {
    	for(Mover m: ghosts) {
    		m.connections = new HashSet<>();
    	}
    	
    	for(Point compareToPoint: wsCurr.points) {
			for(Mover m: ghosts) {
				if(wsCurr.isClearPath(m, compareToPoint)) m.connections.add(compareToPoint);
				if(wsCurr.isClearPath(m, player)) m.connections.add(player);
			}
    	}
    }
    
    void updateWallConnections() {
    	for(Point compareToPoint: wsCurr.points) {
			if(wsCurr.isClearPath(player, compareToPoint) && !compareToPoint.connections.contains(player)) {
				compareToPoint.connections.add(player);
			} else if(compareToPoint.connections.contains(player) && !wsCurr.isClearPath(player, compareToPoint)) {
				compareToPoint.connections.remove(player);
			}
    	}
    }
    
    void displayConnections() {
    	for(Point point: wsCurr.points) {
    		for(Point connec: point.connections) {
    			strokeWeight(2);
    			stroke(200,100,90);
    			line(point.x, point.y, connec.x, connec.y);
    		}
    	}
    }
    
    void ghostConnections() {
    	// purely for me
    	for(Mover m: ghosts) {
    		for(Point connec: m.connections) {
    			strokeWeight(2);
    			stroke(255,255,150);
    			line(m.x, m.y, connec.x, connec.y);
    		}
    	}
    }
    
    void displayGhostDirections() {
    	for(Mover m: ghosts) {
    		Point prevPoint = m;
    		for(int p = m.index; p < m.directions.size(); p++) { // interfaces are quite annoying
    			strokeWeight(2);								 // ^ i dont remember why i said this.
    			stroke(0,255,255);
    			Point pPoint = m.directions.get(p);
    			line(prevPoint.x, prevPoint.y, pPoint.x, pPoint.y);
    			prevPoint = pPoint;
    		}
    	}
    }
  
    // reset player/enemy positions
    void resetPlayers() {
      player = new Player(Pathfinder.this);
      ghosts = new Mover[]{
        // new Mover(Pathfinder.this, new MoveTo(player)),
        // new Mover(Pathfinder.this, new MoveTo(mouse)),
        // new Mover(Pathfinder.this, new RandomMovement()),
        new Mover(Pathfinder.this, new breadthFirstSearch(player)),
        new Mover(Pathfinder.this, new depthFirstSearch(player)),
    	new Mover(Pathfinder.this, new Dijkstra(player)),
    	new Mover(Pathfinder.this, new RandomizedMovement(player))
      };
      start = millis();
    }
  }
}
 
