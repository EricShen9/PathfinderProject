import java.util.*;


// interface allowing specification of movement rules
interface MoveRule {
  void move(Mover m);
}

// rule where no movement is ever done
class StandStill implements MoveRule {
  public void move(Mover m) {};
  public List<Point> findDirections(Mover m){
	  return new ArrayList<>();
  }
}

// rule to move in random direction at all times
class RandomMovement implements MoveRule {
  public void move(Mover m) {
    double angle = Math.random() * Math.PI * 2;
    m.moveTo(m.x + Math.cos(angle) * m.speed,
             m.y + Math.sin(angle) * m.speed);
  }
  public List<Point> findDirections(Mover m){
	  return new ArrayList<>();
  }
}

// rule to move towards a Point (which could be moving itself)
class MoveTo implements MoveRule {
  Point target;
  
  MoveTo(Point target) {
    this.target = target;
  }
  
  public void move(Mover m) {
    m.moveTo(target);
  }
  public List<Point> findDirections(Mover m){
	  return new ArrayList<>();
  }
}

class breadthFirstSearch implements MoveRule{
	Point player;
	
	breadthFirstSearch(Point target) {
		this.player = target;
	}
	
	public void move(Mover m) {
		m.directions = findDirections(m);
		if(m.directions.size() == 0) return;
		m.moveTo(m.directions.get(1));
	}
	
	// doesn't need to account for distance, just amount of nodes
	public List<Point> findDirections(Mover m){ 
	    Deque<List<Point>> queue = new ArrayDeque<>();
	    List<Point> list = new ArrayList<>();
	    list.add(m);
	    queue.add(list);
	    while(!queue.isEmpty()){
	        List<Point> path = queue.remove();
	        Point recentNode = path.get(path.size() -1);
	        
	        // System.out.println(path);
	        if(recentNode == player) return path;
	        for(Point p: recentNode.connections){
	            List<Point> copyList = new ArrayList<>(path);
	            if(copyList.contains(p)) continue;
	            copyList.add(p);
	            queue.add(copyList);
	        }
	    }
	    return new ArrayList<>();
	}
}

class depthFirstSearch implements MoveRule{
	Point player;
	
	depthFirstSearch(Point target) {
		this.player = target;
	}
	
	public void move(Mover m) {
		if(m.directions.size() <= 1) { // probably shoudlve made a variable for m.directions, 
			m.directions = findDirections(m); // like i made one called current which i only use twice
			m.index = 1;
		}
		if(m.directions.size() == 0) return; // im ngl idk if this even does anything but it's useful	
											 // update: it's useful.
		Point current = m.directions.get(m.index);
		if(m.distTo(current) == 0 && m.index < m.directions.size() - 1) m.index++;
		m.moveTo(current);
		if(m.index == m.directions.size() - 1  && !m.connections.contains(player)) {
			m.directions = findDirections(m);
			m.index = 1;
		}
	}

	List<Point> findDirections(Mover m){
		Deque<List<Point>> stack = new ArrayDeque<>();
	    List<Point> list = new ArrayList<>();
	    HashSet<Point> visited = new HashSet<>();
	    list.add(m);
	    stack.push(list);
	    
	    while(!stack.isEmpty()){
	    	List<Point> path = stack.remove();
	    	Point recentNode = path.get(path.size() -1);
	    	
	    	if(visited.contains(recentNode)) continue;
	    	visited.add(recentNode);
	    	if(recentNode == player) return path;
	    	
	    	for(Point p: recentNode.connections){
	            List<Point> copyList = new ArrayList<>(path);
	            if(copyList.contains(p)) continue;
	            copyList.add(p);
	            stack.push(copyList);
	        }
	    }
	    return new ArrayList<>();
	}
}

class RandomizedMovement implements MoveRule {
	Point player;
	
	RandomizedMovement(Point target){
		this.player = target;
	}
	
	public void move(Mover m) {
		if(m.directions.size() <= 1) {
			m.directions = findDirections(m);
			m.index = 1;
		}
		if(m.directions.size() == 0) return;
		
		Point current = m.directions.get(m.index);
		if(m.distTo(current) == 0 && m.index < m.directions.size() - 1) m.index++;
		if(m.index == m.directions.size() - 1 && !m.connections.contains(player)) {
			m.directions = findDirections(m);
			m.index = 1;
		}
		m.moveTo(current);
	}
	
	public List<Point> findDirections(Mover m){
		List<Point> path = new ArrayList<>();
		HashSet<Point> visited = new HashSet<>();
		Point point = m;
		while(true) {
			path.add(point);
			if(point == player) return path;
			List<Point> points = new ArrayList<>();
			for(Point n: point.connections) {
				if(visited.contains(n)) continue;
				points.add(n);
			}
			if(points.size() == 0) break;
			Point random = points.get((int) (Math.random() * points.size()));
			point = random;
			visited.add(random);
		}
		
		return new ArrayList<>();
	}
	
}

class Dijkstra implements MoveRule{
	Point player;
	
	Dijkstra(Point target) {
		this.player = target;
	}
	// indentation got weird, makes me sad.
	  public void move(Mover m) {
			m.directions = findDirections(m);
			if(m.directions.size() == 0) return;
			if(m.distTo(m.directions.get(0)) == 0 && m.directions.size() > 2) m.directions.remove(0);
			m.moveTo(m.directions.get(0));
	  }
	  
	  public List<Point> findDirections(Mover m){
		  PriorityQueue<Step> paths = new PriorityQueue<>();
		  HashSet<Point> visited = new HashSet<>();
		  paths.add(new Step(m, 0, new ArrayList<>()));
		  
	      while(paths.size() > 0) {
	    	  Step s = paths.poll();
	    	  if(s.to == player) return s.directions;
	    	  if(visited.contains(s.to)) continue;
	    	  visited.add(s.to);
	        	
	    	  for(Point n2: s.to.connections) {
	    		  
	    		  float n2Dist = (float) (s.dist + s.to.distTo(n2)); 
	        	  // System.out.println(n2Dist);
	        	  List<Point> n2Dir = new ArrayList<>(s.directions);
	        	  n2Dir.add(n2);
	        	  paths.add(new Step(n2, n2Dist, n2Dir));
	    	  }
	       }
		  return new ArrayList<>();
	  }
}

class Step implements Comparable<Step>{
    Point to;
    float dist;
    List<Point> directions = new ArrayList<>();
    
    Step(Point t, float d) {
        to = t;
        dist = d;
    }
    
    Step(Point t, float d, List<Point> dir) {
        to = t;
        dist = d;
        directions = dir;
    }
    
    public int compareTo(Step other) {
    	if(this.dist < other.dist) return -1;
    	if(this.dist > other.dist) return 1;
    	return 0;
    }
    
}

/*
    void dijkstra(Node start) {
        PriorityQueue<Step> toTake = new PriorityQueue<>();
        purple.clear();
        green.clear();
        toTake.add(new Step(null, start, 0));
        
        while(toTake.size() > 0) {
        	Step s = toTake.poll();
        	Node n = s.to;
        	if(purple.containsKey(n)) continue;
        	purple.put(n, s.from);
        	
        	for(Node n2: n.neighbors) {
        		float n2Dist = s.dist + dist(n.x, n.y, n2.x, n2.y); 
        		toTake.add(new Step(n, n2, n2Dist));
        	}
        }
    }
 */

