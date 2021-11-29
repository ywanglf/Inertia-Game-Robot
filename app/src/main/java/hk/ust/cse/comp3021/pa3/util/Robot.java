package hk.ust.cse.comp3021.pa3.util;

import hk.ust.cse.comp3021.pa3.model.Direction;
import hk.ust.cse.comp3021.pa3.model.GameState;
import hk.ust.cse.comp3021.pa3.model.MoveResult;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Robot is an automated worker that can delegate the movement control of a player.
 * <p>
 * It implements the {@link MoveDelegate} interface and
 * is used by {@link hk.ust.cse.comp3021.pa3.view.panes.GameControlPane#delegateControl(MoveDelegate)}.
 */
public class Robot implements MoveDelegate {
    public enum Strategy {
        Random, Smart
    }

    /**
     * A generator to get the time interval before the robot makes the next move.
     */
    //public static Generator<Long> timeIntervalGenerator = TimeIntervalGenerator.everySecond();
    public static Generator<Long> timeIntervalGenerator = TimeIntervalGenerator.veryFast();
    //public static Generator<Long> timeIntervalGenerator = TimeIntervalGenerator.expectedMilliseconds(50);
    /**
     * e.printStackTrace();
     * The game state of thee.printStackTrace(); player that the robot delegates.
     */
    private final GameState gameState;

    /**
     * The strategy of this instance of robot.
     */
    private final Strategy strategy;

    private static final AtomicBoolean running = new AtomicBoolean(false);

    public Robot(GameState gameState) {
        this(gameState, Strategy.Smart);
    }

    public Robot(GameState gameState, Strategy strategy) {
        this.strategy = strategy;
        this.gameState = gameState;
    }

    public Thread thread;
    /**
     * TODO Start the delegation in a new thread.
     * The delegation should run in a separate thread.
     * This method should return immediately when the thread is started.
     * <p>
     * In the delegation of the control of the player,
     * the time interval between moves should be obtained from {@link Robot#timeIntervalGenerator}.
     * That is to say, the new thread should:
     * <ol>
     *   <li>Stop all existing threads by calling {@link Robot#stopDelegation()}</li>
     *   <li>Start a new thread. And inside the thread:</li>
     *   <ul>
     *      <li>Wait for some time (obtained from {@link TimeIntervalGenerator#next()}</li>
     *      <li>Make a move, call {@link Robot#makeMoveRandomly(MoveProcessor)} or
     *      {@link Robot#makeMoveSmartly(MoveProcessor)} according to {@link Robot#strategy}</li>
     *      <li>repeat</li>
     *   </ul>
     * </ol>
     * The started thread should be able to exit when {@link Robot#stopDelegation()} is called.
     * <p>
     *
     * @param processor The processor to make movements.
     */
    @Override
    public void startDelegation(@NotNull MoveProcessor processor) {

        stopDelegation();
        System.out.println("Start Delegation - Initial no. of threads: "+Thread.activeCount());
        running.set(true);


        thread = new Thread(() -> {
            synchronized (thread){

                while(running.get()){
                    System.out.println("----> running: "+Thread.currentThread().getName()+ "; num: "+Thread.activeCount());
                    try {
                        System.out.println("waiting...");
                        thread.wait(timeIntervalGenerator.next());
                        //Thread.currentThread().sleep(timeIntervalGenerator.next());
                    } catch (InterruptedException e)
                    {
                        running.set(false);
                        System.out.println(thread+(thread.isAlive()?" is Alive!!!!!":" is Dead"));
                        System.out.println("Thread was interrupted, Failed to complete operation");
                        return;
                    }
                    if (running.get() && strategy == Strategy.Random){
                        makeMoveRandomly(processor);
                        System.out.println("|| Player "+ gameState.getPlayer().getId()+" Gem: "+gameState.getNumGotGems());
                        System.out.println("|| Player "+ gameState.getPlayer().getId()+" Life: "+(gameState.getNumLives()-1));
                    }
                    else if (running.get() && strategy == Strategy.Smart){
                        makeMoveSmartly(processor);
                        System.out.println("|| Player "+ gameState.getPlayer().getId()+" Gem: "+gameState.getNumGotGems());
                        System.out.println("|| Player "+ gameState.getPlayer().getId()+" Life: "+(gameState.getNumLives()-1));
                    }
                }
                return;
            }

        });

        thread.start();
    }

    /**
     * TODO Stop the delegations, i.e., stop the thread of this instance.
     * When this method returns, the thread must have exited already.
     */
    @Override
    public void stopDelegation() {

        running.set(false);


        if (thread != null && thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(">>>> waiting exit");
        }
        if (thread != null && !thread.isAlive()) {
            System.out.println(">>>>> thread exited");
        }





        System.out.println("--- "+running.get()+" Stop Delegation ---");
        System.out.println("Final num of threads: "+Thread.activeCount());
    }

    private MoveResult tryMove(Direction direction) {
        var player = gameState.getPlayer();
        if (player.getOwner() == null) {
            return null;
        }
        var r = gameState.getGameBoardController().tryMove(player.getOwner().getPosition(), direction, player.getId());
        return r;
    }

    private int tryMoveSmartly(Direction direction) {
        var player = gameState.getPlayer();
        if (player.getOwner() == null) {
            return -1;
        }
        var r = gameState.getGameBoardController().tryMoveSmartly(player.getOwner().getPosition(), direction, player.getId());
        return r;
    }

    /**
     * The robot moves randomly but rationally,
     * which means the robot will not move to a direction that will make the player die if there are other choices,
     * but for other non-dying directions, the robot just randomly chooses one.
     * If there is no choice but only have one dying direction to move, the robot will still choose it.
     * If there is no valid direction, i.e. can neither die nor move, the robot do not perform a move.
     * <p>
     * TODO modify this method if you need to do thread synchronization.
     *
     * @param processor The processor to make movements.
     */
    private synchronized void makeMoveRandomly(MoveProcessor processor) {
        System.out.println("= Robot - makeMoveRandomly: "+Thread.currentThread().getName());
        var directions = new ArrayList<>(Arrays.asList(Direction.values()));
        Collections.shuffle(directions);
        Direction aliveDirection = null;
        Direction deadDirection = null;
        for (var direction :
                directions) {
            var result = tryMove(direction);
            if (result instanceof MoveResult.Valid.Alive) {
                aliveDirection = direction;
            } else if (result instanceof MoveResult.Valid.Dead) {
                deadDirection = direction;
            }
        }
        if (aliveDirection != null) {
            //System.out.println("Offset: "+aliveDirection.getRowOffset()+", "+ aliveDirection.getColOffset());
            processor.move(aliveDirection);
        } else if (deadDirection != null) {
            processor.move(deadDirection);
        }
    }

    /**
     * TODO implement this method
     * The robot moves with a smarter strategy compared to random.
     * This strategy is expected to beat random strategy in most of the time.
     * That is to say we will let random robot and smart robot compete with each other and repeat many (>10) times
     * (10 seconds timeout for each run).
     * You will get the grade if the robot with your implementation can win in more than half of the total runs
     * (e.g., at least 6 when total is 10).
     * <p>
     *
     * @param processor The processor to make movements.
     */
    private void makeMoveSmartly(MoveProcessor processor) {

        System.out.println("1. Make Move Smartly: "+Thread.currentThread().getName());
        var directions = new ArrayList<>(Arrays.asList(Direction.values()));
        Collections.shuffle(directions);
        Direction aliveDirection = null;
        Direction deadDirection = null;
        boolean hasPlayer = false;
        int initResult = 1; // no movement

        for (var direction :
                directions) {

            int result = tryMoveSmartly(direction);
            System.out.println("2. >>>>> moving direction: "+direction.name()+result);

            // if (result == 2) // gem or life
            // if (result == 1) // nth
            // if (result == 0) // mine
            // if (result == -1) // invalid
            // if (result == -2) // player

            // if result < previous one
            if (result >= initResult) {
                initResult = result;
                aliveDirection = direction;
            } else if (result == 0) {
                deadDirection = direction;
            } else if (result == -2){
                hasPlayer = true;
            }
        }
        if (aliveDirection != null) {
            //System.out.println("Offset: "+aliveDirection.getRowOffset()+", "+ aliveDirection.getColOffset());
            processor.move(aliveDirection);
        } else if (deadDirection != null) {
            if (! hasPlayer)
                processor.move(deadDirection);
        }
    }

}
