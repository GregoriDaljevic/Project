import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import static java.lang.Math.random;

/**
 * Implements main game flow Run game for two bots
 */
public class SnakeGame {
	private static final String LOG_FILE = "log.txt";
	private static final long TIMEOUT_THRESHOLD = 1;// timeout threshold for taking a decision in seconds
	public final snakes.Snake snake0, snake1;
	public final snakes.Coordinate mazeSize;
	private final snakes.Bot bot0, bot1;
	private final Random rnd = new Random();
	public snakes.Coordinate appleCoordinate;
	public String gameResult = "0 - 0";
	public int appleEaten0 = 0;
	public int appleEaten1 = 0;
	private int numIterationsAppleNotEaten;
	private int snakeSize;
	public String name0, name1;
	public long startTime;

	private snakes.SnakesRunner bot0_runner, bot1_runner;

	/**
	 * Constructs SnakeGame class
	 *
	 * @param mazeSize size of the game board
	 * @param head0    initial coordinate of first snake's head
	 * @param tailDir0 initial direction of first snake's tail
	 * @param head1    initial coordinate of second snake's head
	 * @param tailDir1 initial direction of first snake's tail
	 * @param size     initial length of snakes
	 * @param bot0     first smart snake bot
	 * @param bot1     second smart snake bot
	 */
	public SnakeGame(snakes.Coordinate mazeSize, snakes.Coordinate head0, snakes.Direction tailDir0, snakes.Coordinate head1, snakes.Direction tailDir1,
	                 int size, snakes.Bot bot0, snakes.Bot bot1) {
		snakeSize = size;
		this.startTime = System.currentTimeMillis();
		this.mazeSize = mazeSize;
		this.snake0 = new snakes.Snake(head0, tailDir0, size, mazeSize);
		this.snake1 = new snakes.Snake(head1, tailDir1, size, mazeSize);
		this.bot0 = bot0;
		this.bot1 = bot1;
		this.name0 = bot0.getClass().getSimpleName();
		this.name1 = bot1.getClass().getSimpleName();

		appleCoordinate = randomNonOccupiedCell();

		this.bot0_runner = new snakes.SnakesRunner(bot0, snake0, snake1, mazeSize, appleCoordinate);
		this.bot1_runner = new snakes.SnakesRunner(bot1, snake1, snake0, mazeSize, appleCoordinate);
	}

	private static snakes.Coordinate getRandom(snakes.Coordinate mazeSize) {
		return new snakes.Coordinate(
				(int) Math.round(random() * mazeSize.x),
				(int) Math.round(random() * mazeSize.y)
		);
	}

	public static SnakeGame makeRandom(snakes.Bot[] bots) {
		snakes.Coordinate mazeSize = new snakes.Coordinate(20, 20);
		return new SnakeGame(
				mazeSize,// größe von spielfeld
//				getRandom(mazeSize), // schlange 1
				new snakes.Coordinate(8, 10),
				snakes.Direction.UP,
//				getRandom(mazeSize),// schlange 2
				new snakes.Coordinate(12, 10),
				snakes.Direction.UP, 4,
				bots[0],
				bots[1]
		);

	}

	/**
	 * Converts game to string representation
	 *
	 * @return game state as a string
	 */
	public String toString() {
		char[][] cc = new char[mazeSize.x][mazeSize.y];
		for (int x = 0; x < mazeSize.x; x++)
			for (int y = 0; y < mazeSize.y; y++)
				cc[x][y] = '.';

		// Coordinate of head of first snake on board
		snakes.Coordinate h0 = snake0.getHead();
		cc[h0.x][h0.y] = 'h';

		// Coordinate of head of second snake on board
		snakes.Coordinate h1 = snake1.getHead();
		cc[h1.x][h1.y] = 'H';

		Iterator<snakes.Coordinate> it = snake0.body.stream().skip(1).iterator();
		while (it.hasNext()) {
			snakes.Coordinate bp = it.next();
			cc[bp.x][bp.y] = 'b';
		}

		it = snake1.body.stream().skip(1).iterator();
		while (it.hasNext()) {
			snakes.Coordinate bp = it.next();
			cc[bp.x][bp.y] = 'B';
		}

		cc[appleCoordinate.x][appleCoordinate.y] = 'X';

		StringBuilder sb = new StringBuilder();
		for (int y = mazeSize.y - 1; y >= 0; y--) {
			for (int x = 0; x < mazeSize.x; x++)
				sb.append(cc[x][y]);
			if (y != 0)
				sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Outputs text to stdout and file
	 *
	 * @param text text that should be displayed
	 */
	private void output(String text) {
		if (true) return;

		FileWriter fw;
		// System.out.println(text);
		try {
			fw = new FileWriter(LOG_FILE, true);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		try {
			fw.write(text + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Run one game step, return whether to continue the game
	 *
	 * @return whether to continue the game
	 */
	public boolean runOneStep() throws InterruptedException {
		//output(toString());

		// the first bot takes a decision of next move

		bot0_runner.apple = appleCoordinate;
		Thread bot0_thread = new Thread(bot0_runner);

		bot0_thread.start();
		bot0_thread.join();
//		bot0_thread.join(TIMEOUT_THRESHOLD * 1000);
		boolean s0timeout = false;
		if (bot0_thread.isAlive()) {
			bot0_thread.interrupt();
			s0timeout = true;
			System.out.println(bot0.getClass().getSimpleName() + " took too long to make a decision");
		}

		snakes.Direction d0 = bot0_runner.chosen_direction;

		// the second bot takes a decision of next move
		bot1_runner.apple = appleCoordinate;
		Thread bot1_thread = new Thread(bot1_runner);

		bot1_thread.start();
//		bot1_thread.join(TIMEOUT_THRESHOLD * 1000);
		bot1_thread.join();

		boolean s1timeout = false;
		if (bot1_thread.isAlive()) {
			bot1_thread.interrupt();
			s1timeout = true;
			System.out.println(bot1.getClass().getSimpleName() + " took too long to make a decision");
		}

		snakes.Direction d1 = bot1_runner.chosen_direction;

		/*
		 * Stopping game condition - one of the snakes decides what it's next move too
		 * long
		 */
		boolean timeout = s0timeout || s1timeout;
		if (timeout) {
			gameResult = "";
			String result = (s0timeout ? 0 : 1) + " - " + (s1timeout ? 0 : 1);
			gameResult += result;
			return false;
		}

		output("snake0->" + d0 + ", snake1->" + d1);
		output("Apples eaten: " + appleEaten0 + " - " + appleEaten1);

		// var grow = move % 3 == 2;
		boolean grow0 = snake0.getHead().moveTo(d0).equals(appleCoordinate);
		boolean grow1 = snake1.getHead().moveTo(d1).equals(appleCoordinate);

//        boolean wasGrow = grow0;
		boolean wasGrow = grow0 || grow1;

		boolean s0dead = !snake0.moveTo(d0, grow0);
//        boolean s1dead = false;
		boolean s1dead = !snake1.moveTo(d1, grow1);

		if (wasGrow || appleCoordinate == null) {
			appleEaten0 = snake0.body.size() - snakeSize;
			appleEaten1 = snake1.body.size() - snakeSize;
			appleCoordinate = randomNonOccupiedCell();
		} else {
			// Apple must change place if not eaten after 10 iterations
			if (numIterationsAppleNotEaten == 10) {
				// reset counter and change apple
				numIterationsAppleNotEaten = 0;
				appleCoordinate = randomNonOccupiedCell();
			} else
				numIterationsAppleNotEaten++;
		}
		s0dead |= snake0.headCollidesWith(snake1);
		s1dead |= snake1.headCollidesWith(snake0);

		/*
		 * stopping game condition - one of snakes collides with something
		 */
		boolean cont = !(s0dead || s1dead);
//        boolean cont = !(s0dead);

		if (!cont) {
			gameResult = "";
			String result = "0 - 0";
			if (s0dead ^ s1dead)
				result = (s0dead ? 0 : 1) + " - " + (s1dead ? 0 : 1);
			else if (s0dead && s1dead)
				result = (appleEaten0 > appleEaten1 ? 1 : 0) + " - " + (appleEaten1 > appleEaten0 ? 1 : 0);

			gameResult += result;
		}
		return cont;
	}

	/**
	 * Run the game
	 */
	public void run() throws InterruptedException {
		while (runOneStep())
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}

		output(gameResult);
	}

	/**
	 * Selects random non-occupied cell of maze
	 *
	 * @return random non-occupied coordinate of the game board
	 */
	private snakes.Coordinate randomNonOccupiedCell() {
		while (true) {
			snakes.Coordinate c = new snakes.Coordinate(rnd.nextInt(mazeSize.x), rnd.nextInt(mazeSize.y));
			if (snake0.elements.contains(c))
				continue;
			if (snake1.elements.contains(c))
				continue;

			return c;
		}
	}
}
