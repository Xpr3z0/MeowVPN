package de.blinkt.openvpn.activities.mine;

import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
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
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.fragments.Utils;

public class VPNProfileAdapter extends RecyclerView.Adapter<VPNProfileAdapter.VPNProfileViewHolder> {

    private List<VpnProfile> vpnProfiles;
    private Context context;
    private OnItemClickListener onItemClickListener;
    private VpnProfile defaultVPN;
    private String lastStatusMessage;

    public interface OnItemClickListener {
        void onItemClick(VpnProfile profile);
        void onEditClick(VpnProfile profile);
    }

    public VPNProfileAdapter(Context context, List<VpnProfile> vpnProfiles, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.vpnProfiles = vpnProfiles;
        this.onItemClickListener = onItemClickListener;
    }

    public void setDefaultVPN(VpnProfile defaultVPN) {
        this.defaultVPN = defaultVPN;
    }

    public void setLastStatusMessage(String lastStatusMessage) {
        this.lastStatusMessage = lastStatusMessage;
    }

    @NonNull
    @Override
    public VPNProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.vpn_list_item, parent, false);
        return new VPNProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VPNProfileViewHolder holder, int position) {
        VpnProfile profile = vpnProfiles.get(position);
        holder.vpnItemTitle.setText(profile.getName());

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(profile));
        holder.quickEditSettings.setOnClickListener(v -> onItemClickListener.onEditClick(profile));

        SpannableStringBuilder warningText = Utils.getWarningText(context, profile);

        if (profile == defaultVPN) {
            if (warningText.length() > 0) {
                warningText.append(" ");
            }
            warningText.append(new SpannableString("Default VPN"));
        }

        if (profile.getUUIDString().equals(VpnStatus.getLastConnectedVPNProfile())) {
            holder.vpnItemSubtitle.setText(lastStatusMessage);
            holder.vpnItemSubtitle.setVisibility(View.VISIBLE);
        } else {
            holder.vpnItemSubtitle.setText(warningText);
            if (warningText.length() > 0) {
                holder.vpnItemSubtitle.setVisibility(View.VISIBLE);
            } else {
                holder.vpnItemSubtitle.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return vpnProfiles.size();
    }

    public static class VPNProfileViewHolder extends RecyclerView.ViewHolder {
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
}
