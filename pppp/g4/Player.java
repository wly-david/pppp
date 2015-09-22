package pppp.g4;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	// see details below
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private Point[] random_pos = null;
	private Random gen = new Random();
	private int dst_no = 0;
	private int total_regions = 0;
	Boolean[] completed_sweep = null;
    private Cell[] grid = null;

	// create move towards specified destination
	private static Move move(Point src, Point dst, boolean play) {
		double dx = dst.x - src.x;
		double dy = dst.y - src.y;
		double length = Math.sqrt(dx * dx + dy * dy);
		double limit = play ? 0.1 : 0.5;
		if (length > limit) {
			dx = (dx * limit) / length;
			dy = (dy * limit) / length;
		}
		return new Move(dx, dy, play);
	}

	// generate point after negating or swapping coordinates
	private static Point point(double x, double y,
							   boolean neg_y, boolean swap_xy) {
		if (neg_y) y = -y;
		return swap_xy ? new Point(y, x) : new Point(x, y);
	}

	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
					 Point[][] pipers, Point[] rats) {
		this.id = id;
		this.side = side;
		int n_pipers = pipers[id].length;
		pos = new Point[n_pipers][8];
		random_pos = new Point[n_pipers];
		pos_index = new int[n_pipers];

		completed_sweep = new Boolean[n_pipers];
		Arrays.fill(completed_sweep, Boolean.FALSE);

        this.grid = create_grid(this.side);

		for (int p = 0; p != n_pipers; ++p) {
			// spread out at the door level
			double door = 0.0;
			if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap = id == 1 || id == 3;
			Point before_gate = point(door, side * 0.5 * .85, neg_y, swap);
			Point inside_gate = point(door, side * 0.5 * 1.2, neg_y, swap);// first and third position is at the door
			double distance = side/2;
			double theta = Math.toRadians(p * 90.0 / (n_pipers - 1) + 45);

			// pos[p][0] = point(door, side * 0.5, neg_y, swap);
            pos[p][0] = point(door, side * 0.5, neg_y, swap);

			pos[p][1] = point(distance * Math.cos(theta), distance + (-1) * distance * Math.sin(theta), neg_y, swap);
            // pos[p][1] = point(distance * Math.cos(theta), distance * Math.sin(theta), neg_y, swap);

			pos[p][2] = before_gate;
			pos[p][3] = inside_gate;
			pos[p][4] = before_gate;

			// second position is chosen randomly in the rat moving areaons;
			pos[p][5] = null;

			// fourth and fifth positions are outside the rat moving area
			pos[p][6] = before_gate;
			pos[p][7] = inside_gate;
			// start with first position
			pos_index[p] = 0;
			dst_no = 0;
		}
	}

    public Cell[] create_grid(int side) {
        /*
         Returns a Cell[] array of length = number of cells = side/20 * side/20
         */

        int cell_side = 5;
        int dim = 0;
        if (side % cell_side == 0)
            dim = side/cell_side;
        float half = side/2;
        Cell[] grid = new Cell[dim*dim];
        
        for(int i=0; i < dim; i++) {
            for(int j=0; j < dim; j++) {
                Cell cell = new Cell(
                                     new Point(  // X, Y - center
                                               (i + 0.5) * cell_side - half,
                                               (j + 0.5) * cell_side - half
                                               ));
                grid[(i * dim) + j] = cell;
            }
        }
        
        Cell.counter = 0;
        return grid;
    }
    
    public void display_grid() {
        for (int i=0; i < this.grid.length; i++) {
            this.grid[i].display_cell();
            System.out.println();
        }
    }
    
    public Cell find_cell(Point rat) {
        for (int i=0; i<this.grid.length; i++) {
            Cell cell = this.grid[i];
            double x1 = cell.center.x - cell.side/2;
            double x2 = cell.center.x + cell.side/2;
            double y1 = cell.center.y + cell.side/2;
            double y2 = cell.center.y - cell.side/2;
            
            if (rat.x >= x1 && rat.x <= x2 && rat.y >= y2 && rat.y <= y1) {
                return cell;
            }
        }
        return null;
    }
    
    public void update_grid_weights(Point[] rats) {
        for (int i=0; i < this.grid.length; i++) {
            this.grid[i].weight = 0;
        }
        
        for (Point rat: rats) {
            System.out.println("Rat is at: " + rat.x + ", " + rat.y);
            Cell cell = find_cell(rat);
            System.out.println("Rat found at: " + cell.center.x + ", " + cell.center.y);

            if (cell != null)
                cell.weight++;
        }
    }
    
    public Map<Integer, Point> get_piper_to_cell(int remaining_pipers) {
        Cell[] grid_copy = Arrays.copyOf(grid, grid.length);
        for (int j=0; j < grid.length; j++) {
            // System.out.println(grid[j].weight + " " + grid_copy[j].weight);
        }
        // new Cell[grid.length];
        // for (int j=0; j<grid.length; j++) {
        // 	grid_copy[j] = grid[j];
        // }
        
        Map<Integer, Point> piper_to_cell = new HashMap<Integer, Point>();
        List<Integer> all_pipers = new ArrayList<Integer>();
        
        for (int i=0; i<remaining_pipers; i++) {
            all_pipers.add(i);
        }
        
        int i;
        int cells_to_consider;
        int sum;
        int avg;
        int n_p_to_i;
        int piper;
        List<Cell> non_zero_cells = new ArrayList<Cell>();
        Iterator<Cell> iter_list;

        for (int k=0; k < grid_copy.length; k++) {
            non_zero_cells.add(grid_copy[k]);
        }
        
        cells_to_consider = remaining_pipers;
        while (remaining_pipers > 0) {

            // cells_to_consider = remaining_pipers;

            int prev_length = non_zero_cells.size();
            if (prev_length > cells_to_consider) {
                // System.out.println("$$$$$$$$$$$$$$$$$$$$$$$"+prev_length);
                // for (int k = cells_to_consider; k < prev_length; k++) {
                //     System.out.println("k: "+ k);
                //     non_zero_cells.remove(k);
                //     non_zero_cells.trimToSize();
                // }
                non_zero_cells = non_zero_cells.subList(0, cells_to_consider);
            }
            
            sum = 0;


            System.out.println("1 Remaining: " + remaining_pipers);
            System.out.println("1 Cells to consider: " + cells_to_consider);
            System.out.println("1 Non zero cells: " + non_zero_cells.size());
            for (int k=0; k<non_zero_cells.size(); k++) {
                System.out.println(non_zero_cells.get(k).weight);
            }
            
            iter_list = non_zero_cells.iterator();
            for (i=0; i<cells_to_consider; i++) {
                // System.out.println("Computing avg...");
                // if (grid_copy[i].weight != 0) {
                //     non_zero_cells.append(grid_copy[i])
                //     sum += grid_copy[i].weight;

                // }
                Cell next_item = iter_list.next();
                 if (next_item.weight != 0) {
                    sum += next_item.weight;
                }
                else
                {
                    System.out.println("Before: " + non_zero_cells.size());
                    iter_list.remove();
                    System.out.println("After: " + non_zero_cells.size());
                }
            }
            cells_to_consider = non_zero_cells.size();
            if (cells_to_consider == 0)
                break;
            System.out.println("2 Remaining: " + remaining_pipers);
            System.out.println("2 Cells to consider: " + cells_to_consider);

            avg = sum/cells_to_consider;
            // if (avg < 1) avg = 1;
            System.out.println("Avg: " + avg);
            
            i = 0;
            iter_list = non_zero_cells.iterator();
            Cell this_cell;
            while(i < cells_to_consider) {
                // System.out.println("i: " + i);
                if (iter_list.hasNext())
                    this_cell = iter_list.next();
                else 
                    break;
                
                // n_p_to_i = grid_copy[i].weight/avg;
                n_p_to_i = this_cell.weight/avg;
                System.out.println("n p i: " + n_p_to_i);
                
                Iterator<Integer> iter = all_pipers.iterator();
                for (int j=0; j<n_p_to_i; j++) {
                    if (iter.hasNext())
                    {
                        piper = iter.next();
                        piper_to_cell.put(piper, this_cell.center);
                        iter.remove();
                        remaining_pipers -= 1;
                    }
                }
                this_cell.weight = this_cell.weight % avg;
                if (this_cell.weight == 0){
                    System.out.println("Before: " + non_zero_cells.size());
                    iter_list.remove();
                    System.out.println("After: " + non_zero_cells.size());
                }
                cells_to_consider = non_zero_cells.size();
                if (cells_to_consider == 0)
                    break;
                i++;
                
            }
            
        }

        while (remaining_pipers > 0) {
            Iterator<Integer> iter = all_pipers.iterator();
            if (iter.hasNext()) {
                    piper = iter.next();
                    piper_to_cell.put(piper, grid_copy[0].center);
                    iter.remove();
                    remaining_pipers -= 1;
            }
        }
                
        // for (Map.Entry<Integer, Point> entry : piper_to_cell.entrySet()) {
        //     int key = entry.getKey();
        //     Point value = entry.getValue();
        //     System.out.println(key + " " + value.x + " " + value.y);
        // }
        
        return piper_to_cell;
        
    }

        // Yields the number of rats within range
    static int num_captured_rats(Point piper, Point[] rats) {
        int num = 0;
        for (Point rat : rats)
            num += Utils.distance(piper, rat) <= 10 ? 1 : 0;
        return num;
    }

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
					 Point[] rats, Move[] moves) {
        
        try {
            Map<Integer, Point> piper_to_cell = null;
            update_grid_weights(rats);
            
            // sort the cells in the Cell[] grid in descending order of weight/number_of_rats
            Arrays.sort(this.grid, Collections.reverseOrder());
            piper_to_cell = get_piper_to_cell(pipers[id].length);
            for (Map.Entry<Integer, Point> entry : piper_to_cell.entrySet()) {
                int key = entry.getKey();
                Point value = entry.getValue();
                System.out.println(key + " " + value.x + " " + value.y);
            }

            //p : is the index of piper for current player
            for (int p = 0; p != pipers[id].length; ++p) {
                Point src = pipers[id][p];
                Point dst = pos[p][pos_index[p]];
                
                if (completed_sweep[p] && pos_index[p] == 1)
                {
                    pos_index[p] = 4;
                    //				dst = null; // call new destination function here
                }
                // if null then get random position
                if (dst == null) dst = random_pos[p];
                // if position is reached
                if (Math.abs(src.x - dst.x) < 0.000001 &&
                    Math.abs(src.y - dst.y) < 0.000001) {
                    // discard random position
                    if (dst == random_pos[p]) random_pos[p] = null;
                    // get next position
                    if (++pos_index[p] == pos[p].length){
                        pos_index[p] = 0;
                        completed_sweep[p] = true;
                    }
                    dst = pos[p][pos_index[p]];
                    // generate a new position if random
                    if (dst == null || pos_index[p] == 5) {
                        //					double x = (gen.nextDouble() - 0.5) * side * 0.9;
                        //					double y = (gen.nextDouble() - 0.5) * side * 0.9;
                        random_pos[p] = dst = piper_to_cell.get(id);
                    }
                }
                if (pos_index[p] == 6 && num_captured_rats(pipers[id][p], rats) == 0)
                    pos_index[p] = 5;
                if ((pos_index[p] == 3 || pos_index[p] == 7) && num_captured_rats(pipers[id][p], rats) == 0)
                    pos_index[p] = 4;

                if (pos_index[p] == 5 ) {
                    update_grid_weights(rats);
            
                    // sort the cells in the Cell[] grid in descending order of weight/number_of_rats
                    // Arrays.sort(this.grid, Collections.reverseOrder());
                    // piper_to_cell = get_piper_to_cell(pipers[id].length);
                    random_pos[p] = dst = piper_to_cell.get(id);
                }

                // get move towards position
                moves[p] = move(src, dst, (pos_index[p] > 1 && pos_index[p] < 4) || (pos_index[p] > 5));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
	}
}