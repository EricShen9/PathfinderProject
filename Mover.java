import java.util.ArrayList;
import java.util.List;

import processing.core.PApplet;


/*
 * 
   
SCHEDULE
after winter break: aim to have graph showing as much as possible
   and the moving elements of the graph updated correctly
   IDEALLY - you would've started on one of the ghost moving algorithms
1st week of Jan:
  - debug graph display issues
  - learn about Dijkstra's algorithm
  - work on first ghost movement
weeks 2-4 of Jan:
  - work on 2nd-4th ghost movement

OBJECTIVES
  make the graph out of Points in the world
  - starting with Wall Point-to-Wall Point
  - display the graph!
  - add in connections to and from the Mover Points (Player, ghosts)
  make 4 ghosts that use the graph to plan and execute their movement
  - BFS (least # of degrees/turns in movement before reaching player)
  - Dijkstra's algorithm (learn about this in first week back)
  - random choice of point -> move there -> repeat (remember the point you chose until you get there)
  - DFS
Bonus
  - spicy graphics (images)
  - add a menu
  - 
  
 * 
 * 
 * 
 */
// defines a moving point, aka player or enemy
public class Mover extends Point {
  // rule that will be followed when point moves
  MoveRule rule;
  float speed = 2.0f;  // top speed allowed for point
  float hue;
  List<Point> directions = new ArrayList<>();
  int index = 0; // used only for dijkstra and dfs when displaying connections yknow
  
  // Mover must remember its Pathfinder instance so it can
  //   move without crashing into walls
  Pathfinder pf;
  
  // default mover doesn't actually move
  Mover(Pathfinder pf) {
    this(pf, new StandStill());
  }
  
  // mover with specific movement system/pattern
  Mover(Pathfinder pf, MoveRule rule) {
    super(pf);
    this.pf = pf;
    this.rule = rule;
    hue = (float) (Math.random() * 360);
  }
  
  // movement is based on the rule/system of movement
  void move() {
    rule.move(this);
  }
  
  // attempts to move in direction of another point
  void moveTo(Point p) {
    moveTo(p.x, p.y);
  }
  
  void moveTo(double x2, double y2) {
    moveTo((float)x2, (float)y2);
  }
  
  // attempts to move toward coordinates (x2, y2)
  void moveTo(float x2, float y2) {
    float dx = x2 - x;
    float dy = y2 - y;
    
    double d = Math.sqrt(dx*dx + dy*dy);
    
    // don't move
    if (d == 0) return;
    
    // scale down movement if necessary
    if (d > speed) {
      dx *= speed / d;
      dy *= speed / d;
    }
    
    Point target = new Point(x + dx, y + dy);
    Point crashPoint = crashCheck(target);
    
    // move to target if nothing blocks it
    if (crashPoint == null) {
      x += dx;
      y += dy;
    }
    // move halfway to blockage, unless within 0.5px already
    else if (this.distTo(crashPoint) >= 0.5) {
      x = (x + crashPoint.x) / 2;
      y = (y + crashPoint.y) / 2;
    }
  }
  
  // returns closest Point that would be crashed into by
  //   moving to target, or null if movement is unblocked
  Point crashCheck(Point target) {
    Point closest = null;
    double closestD = Double.MAX_VALUE;
    
    List<Point> crashes = pf.wsCurr.intersections(this, target);
    
    // get closest among all potential crashes
    for (Point crash : crashes) {
        
      double crashD = this.distTo(crash);
      
      // get closest result
      if (crashD < closestD) {
        closest = crash;
        closestD = crashD;
      }
    }
    return closest;
  }
  
  @Override
  void display(PApplet pa) {
    pa.noStroke();
    pa.fill(hue, 100, 100);
    pa.ellipse(x, y, 10, 10);
  }
}

// special Mover which is displayed differently and
//   always follows the mouse
class Player extends Mover {
  Player(Pathfinder pf) {
    super(pf, new MoveTo(pf.mouse));
  }
  
  @Override
  void display(PApplet pa) {
    pa.noStroke();
    pa.fill(hue, 100, 100); // light
    pa.ellipse(x, y, 20, 20);
    pa.fill(hue, 100, 50);  // darker
    pa.ellipse(x, y, 3, 3);
  }
}