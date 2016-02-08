package edu.umass.cs.automan.tools

import java.io.{ObjectOutputStream, FileOutputStream}
import edu.umass.cs.automan.core.policy.aggregation.AdversarialPolicy

object PrecomputeNumToRun extends App {
  val output_filename = "PossibilitiesTable.dat"
  val num_possibilities = 1000
  val num_rewards = 25

  val table = new Table(num_possibilities, num_rewards)

  for (np <- 2 to (num_possibilities + 1);
       reward_cents <- 1 to num_rewards
      ) {
    val q = new StubQuestion(np)
    val policy = new AdversarialPolicy(q)

    val reward: BigDecimal =
      ( BigDecimal(reward_cents)
        / BigDecimal(100)
      ).setScale(2, math.BigDecimal.RoundingMode.FLOOR)
    val ntr: Int = policy.num_to_run(Nil, 0, reward)

    table.addEntry(np, reward, ntr)
  }

  for (np <- 2 to (num_possibilities + 1);
       reward_cents <- 1 to num_rewards
  ) {
    val reward: BigDecimal =
      ( BigDecimal(reward_cents)
        / BigDecimal(100)
        ).setScale(2, math.BigDecimal.RoundingMode.FLOOR)

    table.getEntryOrNone(np, reward) match {
      case Some(ntr) => println(s"$np, $reward: $ntr")
      case None => throw new Exception(s"Entry unexpectedly missing for $np, $reward")
    }
  }

  val os = new ObjectOutputStream(new FileOutputStream(output_filename))
  os.writeObject(table)
  os.close()

//  val is = new ObjectInputStream(new FileInputStream(output_filename))
//  val table2 = is.readObject().asInstanceOf[Table]
//  is.close()
//
//  for (np <- 2 to (num_possibilities + 1);
//       reward_cents <- 1 to num_rewards
//  ) {
//    val reward: BigDecimal =
//      ( BigDecimal(reward_cents)
//        / BigDecimal(100)
//        ).setScale(2, math.BigDecimal.RoundingMode.FLOOR)
//
//    val ntr = table2.getEntry(np, reward)
//
//    println(s"read back: $np, $reward: $ntr")
//  }
}