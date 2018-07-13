package de.int80.gothbingo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

class NumPlayersActionProvider extends ActionProvider {
    private final Context mContext;
    private TextView numPlayersText;

    public NumPlayersActionProvider(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public View onCreateActionView() {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        @SuppressLint("InflateParams")
        View providerView = layoutInflater.inflate(R.layout.provider_num_players, null);

        ViewGroup providerViewGrp = (ViewGroup) providerView;
        numPlayersText = providerViewGrp.findViewById(R.id.numPlayersTextView);

        return providerView;
    }

    void setNumPlayers(int numPlayers) {
        numPlayersText.setText(String.valueOf(numPlayers));
    }
}
