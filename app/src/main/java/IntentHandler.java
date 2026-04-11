import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.al_qari.AlJawfActivity_lp;
import com.example.al_qari.AlHalqActivity_lp;
import com.example.al_qari.AlLisaanActivity_lp;
import com.example.al_qari.AshShafatanActivity_lp;

public class IntentHandler {

    private final Context context;

    public IntentHandler(Context context) {
        this.context = context;
    }

    public void handleIntent(String intent) {
        switch (intent) {

            // Open specific modules
            case "open_module_al_jauf":
                openAlJaufModule();
                break;
            case "open_module_al_halq":
                openAlHalqModule();
                break;
            case "open_module_al_lisaan":
                openAlLisaanModule();
                break;
            case "open_module_ash_shufataan":
                openAshShufataanModule();
                break;

            default:
                Log.d("IntentHandler", "Unknown intent: " + intent);
                break;
        }
    }

    // ------------------ Module Actions ------------------
    private void openAlJaufModule() {
        Intent intent = new Intent(context, AlJawfActivity_lp.class);
        context.startActivity(intent);
    }

    private void openAlHalqModule() {
        Intent intent = new Intent(context, AlHalqActivity_lp.class);
        context.startActivity(intent);
    }

    private void openAlLisaanModule() {
        Intent intent = new Intent(context, AlLisaanActivity_lp.class);
        context.startActivity(intent);
    }

    private void openAshShufataanModule() {
        Intent intent = new Intent(context, AshShafatanActivity_lp.class);
        context.startActivity(intent);
    }
}
