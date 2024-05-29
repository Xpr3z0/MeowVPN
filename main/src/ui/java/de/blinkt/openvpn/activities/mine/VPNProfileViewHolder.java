/*
 * Copyright (c) 2012-2024 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities.mine;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;

public class VPNProfileViewHolder extends RecyclerView.ViewHolder {
    TextView vpnItemTitle;
    TextView vpnItemSubtitle;
    ImageView quickEditSettings;

    public VPNProfileViewHolder(View itemView) {
        super(itemView);
        vpnItemTitle = itemView.findViewById(R.id.vpn_item_title);
        vpnItemSubtitle = itemView.findViewById(R.id.vpn_item_subtitle);
        quickEditSettings = itemView.findViewById(R.id.quickedit_settings);
    }
}


