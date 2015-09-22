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
	private int dst_no=0;
	private int total_regions=0;
	// create move towards specified destination
	private static Move move(Point src, Point dst, boolean play)
	{
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
	                           boolean neg_y, boolean swap_xy)
	{
		if (neg_y) y = -y;
		return swap_xy ? new Point(y, x) : new Point(x, y);
	}

	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
	                 Point[][] pipers, Point[] rats)
	{
		this.id = id;
		this.side = side;
		int n_pipers = pipers[id].length;
		pos = new Point [n_pipers][7];
		random_pos = new Point [n_pipers];
		pos_index = new int [n_pipers];
		total_regions= (n_pipers > 4 ? 4 : n_pipers);

		double maxOffset = (side / n_pipers) * 0.5;

		for (int p = 0 ; p != n_pipers ; ++p) {
			// spread out at the door level
			double door = 0.0;
			if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			// pick coordinate based on where the player is
			boolean neg_y = id == 2 || id == 3;
			boolean swap  = id == 1 || id == 3;
			// first and third position is at the door
			pos[p][0] = pos[p][4] = point(door, side * 0.5, neg_y, swap);
			// second position is chosen randomly in the rat moving area
 
				// dst = random_pos[p];
			int cur_pipers_region=p % total_regions;
			double offset= (p/total_regions)*2;

			if (offset > maxOffset){
				offset = maxOffset;
			}
			boolean offsetPlus=(p/total_regions)%2==0; // plus when its Even else do minus 
			pos[p][2] = point( -side/2 + ( (side/total_regions) * (cur_pipers_region + 0.5) + (offsetPlus?offset : -1*offset )), side*0.9 -side/2, neg_y, swap);
			pos[p][1] = point( (-side/2 + (side/total_regions) * (cur_pipers_region + 0.5) + (offsetPlus?offset : -1*offset )), side*0.35 -side/2 , neg_y, swap);
 
			// fourth and fifth positions are outside the rat moving area
			pos[p][3] = point(door , side * 0.5 *.75, neg_y, swap);
			pos[p][5] = point(door * -9, side * 0.5 + 5, neg_y, swap);
			pos[p][6] = point(door * +9, side * 0.5 + 5, neg_y, swap);
			// start with first position
			pos_index[p] = 0;
			dst_no=0;
		}
	}

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
	                 Point[] rats, Move[] moves)
	{
		//p : is the index of piper for current player..
		for (int p = 0 ; p != pipers[id].length ; ++p) {
			Point src = pipers[id][p];
			Point dst = pos[p][pos_index[p]];
			// if null then get random position
 
			// if position is reached
			if (Math.abs(src.x - dst.x) < 0.000001 &&
			    Math.abs(src.y - dst.y) < 0.000001) {
				// discard random position
				if (dst == random_pos[p]) random_pos[p] = null;
				// get next position
				if (++pos_index[p] == pos[p].length) pos_index[p] = 0;
				dst = pos[p][pos_index[p]];
				// generate a new position if random
				if (dst == null) {
					double x = (gen.nextDouble() - 0.5) * side * 0.9;
					double y = (gen.nextDouble() - 0.5) * side * 0.9;
					random_pos[p] = dst = new Point(x, y);
				}
			}
			// get move towards position
			List<Integer> positions=new ArrayList<>();
////			positions.add(1);
//			positions.add(2);
//			positions.add(3);
//			if(positions.contains(pos_index[p]) && ! Utils.allowMove(id, p , pipers, dst,  total_regions) ){
//				moves[p] = move(src, src, pos_index[p] > 1);
//			}else{
//				moves[p] = move(src, dst, pos_index[p] > 1);
//			}
			moves[p] = move(src, dst, pos_index[p] > 1);
		}
	}
}
