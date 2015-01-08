package is.hello.sense.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.SmartAlarm;

public class SmartAlarmSoundAdapter extends ArrayAdapter<SmartAlarm.Sound> {
    private long selectedSoundId = -1;

    public SmartAlarmSoundAdapter(Context context) {
        super(context, R.layout.item_simple_text);
    }

    public void setSelectedSoundId(long selectedSoundId) {
        this.selectedSoundId = selectedSoundId;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView text = (TextView) super.getView(position, convertView, parent);

        SmartAlarm.Sound sound = getItem(position);
        text.setText(sound.name);

        if (selectedSoundId == sound.id) {
            text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.radio_on, 0);
        } else {
            text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.radio_off, 0);
        }

        return text;
    }
}
