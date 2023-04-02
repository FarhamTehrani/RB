import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimRace {

  public static final Logger LOGGER = LoggerFactory.getLogger(SimRace.class);
  public final int numberOfCars;
  public final int rounds;
  public Queue<Car> track;

  public SimRace(int numberOfCars, int rounds) {
    this.numberOfCars = numberOfCars;
    this.rounds = rounds;
    track = new LinkedList<>();
  }

  public void startRace() {
    for (int i = 0; i < numberOfCars; i++) {
      Car c = new Car(i, rounds);
      track.add(c);
      c.start();
    }

    track.forEach(c -> {
      try {
        c.join();
      } catch (InterruptedException e) {
        LOGGER.warn(e.getMessage());
      }
    });

    track.stream()
        .sorted(Car::compareTo)
        .forEach(c -> LOGGER.info(c.toString()));
  }
}
