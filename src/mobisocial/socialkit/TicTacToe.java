package mobisocial.socialkit;

import java.util.ArrayList;
import java.util.List;

import mobisocial.socialkit.SocialKit.Dungbeetle;
import android.app.Activity;
import android.os.Bundle;

public class TicTacToe extends Activity {
    Dungbeetle mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Dungbeetle.isDungbeetleIntent(getIntent())) {
            mDb = Dungbeetle.getInstance(getIntent());

            mDb.getThread().getMembers(); // List of all known people
            mDb.getThread().getJunction(); // Message-passing without persistence.

            // sendMessage(...);
            // synState(...);
        }
    }

    class Board {
        List<Square> spaces = parseDb(mDb.getThread().getApplicationStorage(null));

        class Square {
            int value;

            public static final int VALUE_EMPTY = 0;
            public static final int VALUE_X= 1;
            public static final int VALUE_O= 2;
        }

        public List<Square> parseDb(Object state) {
            return new ArrayList<TicTacToe.Board.Square>();
            // for each key in state
                // spaces.get(i).value = state.get(whatevs);
        }
    }

    // public void onTouchEvent(something) { 
    /*
         Square square = getSquare(something);
         if (square.value == VALUE_EMPTY && myTurn) {
             square.value = myValue
             mDb.sync(whatevs);
         }
     
     */
    // }
}
