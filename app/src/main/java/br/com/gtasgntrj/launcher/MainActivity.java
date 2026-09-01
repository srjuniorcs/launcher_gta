package br.com.gtasgntrj.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String SERVER_IP = "51.222.193.109";
    private static final int SERVER_PORT = 7777;
    private static final String GAME_PACKAGE = "com.eaglevision.samp";
    private static final String GAME_ACTIVITY = "com.eaglevision.samp.launcher.activity.CheckActivity";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView statusText;
    private TextView playersText;
    private EditText nickInput;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("sgnt_launcher", MODE_PRIVATE);
        setContentView(buildUi());
        refreshServer();
    }

    private View buildUi() {
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(pad, dp(18), pad, dp(20));
        root.setBackgroundColor(Color.rgb(8, 8, 8));

        ImageView logo = new ImageView(this);
        logo.setImageResource(br.com.gtasgntrj.launcher.R.drawable.sgnt_logo);
        logo.setAdjustViewBounds(true);
        root.addView(logo, new LinearLayout.LayoutParams(-1, dp(230)));

        TextView title = text("GTA SGNT RJ", 27, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWrap());

        TextView subtitle = text("SÃO GONÇALO • NITERÓI • RJ", 12, Color.rgb(225, 6, 0), true);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, fullWrap());
        addGap(root, 18);

        LinearLayout serverCard = card();
        TextView serverTitle = text("SERVIDOR OFICIAL", 13, Color.rgb(167,167,167), true);
        serverCard.addView(serverTitle, fullWrap());
        TextView address = text(SERVER_IP + ":" + SERVER_PORT, 18, Color.WHITE, true);
        serverCard.addView(address, fullWrap());

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusText = text("● VERIFICANDO...", 13, Color.rgb(225,6,0), true);
        playersText = text("--/-- jogadores", 13, Color.LTGRAY, false);
        statusRow.addView(statusText, new LinearLayout.LayoutParams(0, -2, 1));
        statusRow.addView(playersText, new LinearLayout.LayoutParams(-2, -2));
        serverCard.addView(statusRow, fullWrap());
        root.addView(serverCard, fullWrap());
        addGap(root, 14);

        TextView nickLabel = text("SEU NICK", 12, Color.rgb(167,167,167), true);
        root.addView(nickLabel, fullWrap());
        nickInput = new EditText(this);
        nickInput.setSingleLine(true);
        nickInput.setTextColor(Color.WHITE);
        nickInput.setHintTextColor(Color.DKGRAY);
        nickInput.setHint("Ex.: Junior_SGNT");
        nickInput.setTextSize(16);
        nickInput.setPadding(dp(14), 0, dp(14), 0);
        nickInput.setBackground(makeRounded(Color.rgb(20,20,20), Color.rgb(70,70,70), 12));
        nickInput.setText(prefs.getString("nickname", ""));
        root.addView(nickInput, new LinearLayout.LayoutParams(-1, dp(52)));
        addGap(root, 14);

        Button play = button("JOGAR AGORA", true);
        play.setOnClickListener(v -> play());
        root.addView(play, new LinearLayout.LayoutParams(-1, dp(58)));
        addGap(root, 10);

        Button refresh = button("ATUALIZAR STATUS", false);
        refresh.setOnClickListener(v -> refreshServer());
        root.addView(refresh, new LinearLayout.LayoutParams(-1, dp(48)));
        addGap(root, 20);

        TextView socialLabel = text("REDES OFICIAIS • @gtasaogoncalo", 12, Color.LTGRAY, true);
        socialLabel.setGravity(Gravity.CENTER);
        root.addView(socialLabel, fullWrap());
        addGap(root, 8);

        LinearLayout socialRow = new LinearLayout(this);
        socialRow.setOrientation(LinearLayout.HORIZONTAL);
        socialRow.setGravity(Gravity.CENTER);
        Button insta = smallButton("INSTAGRAM");
        insta.setOnClickListener(v -> openUrl("https://instagram.com/gtasaogoncalo"));
        Button tiktok = smallButton("TIKTOK");
        tiktok.setOnClickListener(v -> openUrl("https://www.tiktok.com/@gtasaogoncalo"));
        Button youtube = smallButton("YOUTUBE");
        youtube.setOnClickListener(v -> openUrl("https://www.youtube.com/@gtasaogoncalo"));
        socialRow.addView(insta, weighted());
        socialRow.addView(tiktok, weighted());
        socialRow.addView(youtube, weighted());
        root.addView(socialRow, fullWrap());

        addGap(root, 16);
        TextView footer = text("GTA SGNT RJ • Launcher oficial", 11, Color.DKGRAY, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, fullWrap());
        return root;
    }

    private void play() {
        String nick = nickInput.getText().toString().trim();
        if (nick.length() < 3) {
            Toast.makeText(this, "Digite seu nick antes de jogar.", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.edit().putString("nickname", nick).apply();

        // Integração preparada para o cliente-base. Ao incorporar o motor do cliente
        // no mesmo APK, esta chamada pode ser apontada diretamente para a Activity do jogo.
        Intent game = new Intent();
        game.setComponent(new ComponentName(GAME_PACKAGE, GAME_ACTIVITY));
        game.putExtra("nickname", nick);
        game.putExtra("nick", nick);
        game.putExtra("server_ip", SERVER_IP);
        game.putExtra("ip", SERVER_IP);
        game.putExtra("server_port", SERVER_PORT);
        game.putExtra("port", SERVER_PORT);
        game.setData(Uri.parse("samp://" + SERVER_IP + ":" + SERVER_PORT));
        try {
            startActivity(game);
        } catch (Exception e) {
            Intent fallback = getPackageManager().getLaunchIntentForPackage(GAME_PACKAGE);
            if (fallback != null) {
                fallback.putExtra("nickname", nick);
                fallback.putExtra("server_ip", SERVER_IP);
                fallback.putExtra("server_port", SERVER_PORT);
                startActivity(fallback);
            } else {
                Toast.makeText(this,
                        "Cliente SA:MP ainda não integrado nesta versão de desenvolvimento.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshServer() {
        statusText.setText("● VERIFICANDO...");
        statusText.setTextColor(Color.rgb(225,6,0));
        playersText.setText("--/-- jogadores");
        executor.execute(() -> {
            try {
                ServerInfo info = queryServer(SERVER_IP, SERVER_PORT);
                runOnUiThread(() -> {
                    statusText.setText("● ONLINE");
                    statusText.setTextColor(Color.rgb(40, 200, 90));
                    playersText.setText(info.players + "/" + info.maxPlayers + " jogadores");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("● OFFLINE");
                    statusText.setTextColor(Color.rgb(225,6,0));
                    playersText.setText("não respondeu");
                });
            }
        });
    }

    private ServerInfo queryServer(String ip, int port) throws Exception {
        InetAddress addr = InetAddress.getByName(ip);
        byte[] ipBytes = addr.getAddress();
        if (ipBytes.length != 4) throw new IllegalArgumentException("IPv4 necessário");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{'S','A','M','P'});
        out.write(ipBytes);
        out.write(port & 0xFF);
        out.write((port >> 8) & 0xFF);
        out.write('i');
        byte[] request = out.toByteArray();
        byte[] buffer = new byte[2048];
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(1800);
            socket.send(new DatagramPacket(request, request.length, addr, port));
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            int len = response.getLength();
            if (len < 17) throw new IllegalStateException("Resposta inválida");
            ByteBuffer bb = ByteBuffer.wrap(buffer, 11, len - 11).order(ByteOrder.LITTLE_ENDIAN);
            bb.get(); // passworded
            int players = bb.getShort() & 0xFFFF;
            int maxPlayers = bb.getShort() & 0xFFFF;
            int hostnameLen = bb.getInt();
            if (hostnameLen < 0 || hostnameLen > bb.remaining()) hostnameLen = 0;
            byte[] hostnameBytes = new byte[hostnameLen];
            bb.get(hostnameBytes);
            String hostname = new String(hostnameBytes, StandardCharsets.ISO_8859_1);
            return new ServerInfo(players, maxPlayers, hostname);
        }
    }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception ignored) { Toast.makeText(this, url, Toast.LENGTH_SHORT).show(); }
    }

    private LinearLayout card() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(16), dp(14), dp(16), dp(14));
        v.setBackground(makeRounded(Color.rgb(20,20,20), Color.rgb(70,15,15), 14));
        return v;
    }

    private android.graphics.drawable.GradientDrawable makeRounded(int fill, int stroke, int radiusDp) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(false);
        b.setBackground(makeRounded(primary ? Color.rgb(225,6,0) : Color.rgb(20,20,20), primary ? Color.rgb(255,60,55) : Color.rgb(80,80,80), 12));
        return b;
    }

    private Button smallButton(String label) {
        Button b = button(label, false);
        b.setTextSize(10);
        return b;
    }

    private LinearLayout.LayoutParams fullWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(44), 1);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }
    private void addGap(LinearLayout v, int dp) {
        Space s = new Space(this);
        v.addView(s, new LinearLayout.LayoutParams(1, dp(dp)));
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    private static class ServerInfo {
        final int players, maxPlayers;
        final String hostname;
        ServerInfo(int players, int maxPlayers, String hostname) {
            this.players = players;
            this.maxPlayers = maxPlayers;
            this.hostname = hostname;
        }
    }
}
