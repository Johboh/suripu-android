package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.ui.widget.SleepSoundsPlayerView;
import is.hello.sense.util.Constants;
import is.hello.sense.util.IListObject;

public class SleepSounds extends ApiResponse implements IListObject, SleepSoundsPlayerView.ISleepSoundsPlayerRowItem {

    @SerializedName("sounds")
    private List<Sound> sounds;

    @SerializedName("state")
    private State state;

    public List<Sound> getSounds() {
        return sounds;
    }

    public State getState() {
        return state;
    }

    public Sound getSoundWithId(final int id) {
        if (id == Constants.NONE) {
            return null;
        }
        for (final Sound sound : sounds) {
            if (sound.getId() == id) {
                return sound;
            }
        }
        return null;
    }

    @Override
    public List<Sound> getListItems() {
        return sounds;
    }

    @Override
    public int getLabelRes() {
        return R.string.sleep_sounds_sound_label;
    }

    @Override
    public int getImageRes() {
        return R.drawable.icon_sound_24;
    }

    @Override
    public IListObject getListObject() {
        return this;
    }

    public enum State implements Enums.FromString {
        OK,
        SOUNDS_NOT_DOWNLOADED,    // Sounds have not *yet* been downloaded to Sense, but should be.
        SENSE_UPDATE_REQUIRED,    // Sense cannot play sounds because it has old firmware
        FEATURE_DISABLED;         // User doesn't have this feature flipped.

        public static State fromString(final @NonNull String string) {
            return Enums.fromString(string, values(), OK);
        }
    }
}
