package br.com.gtasgntrj.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    // Fica somente no código. O IP NÃO aparece na tela do launcher.
    private static final String SERVER_IP = "51.222.193.109";
    private static final int SERVER_PORT = 7777;

    // Temporário enquanto o cliente próprio ainda não está incorporado.
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        prefs = getSharedPreferences("sgnt_launcher", MODE_PRIVATE);
        setContentView(buildUi());
        refreshServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (statusText != null) refreshServer();
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ImageView background = new ImageView(this);
        background.setImageResource(R.drawable.sgnt_background);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(background, new FrameLayout.LayoutParams(-1, -1));

        View shade = new View(this);
        GradientDrawable shadeDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x22000000, 0x66000000, 0xF4000000});
        shade.setBackground(shadeDrawable);
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(22), dp(44), dp(22), dp(26));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -1));

        // Espaço para deixar a arte respirar, mantendo o visual do fundo como protagonista.
        Space hero = new Space(this);
        content.addView(hero, new LinearLayout.LayoutParams(1, 0, 1.0f));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(roundGradient(0xD9131313, 0xF0080808, 22, 0x55E10600));
        content.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout onlineRow = new LinearLayout(this);
        onlineRow.setOrientation(LinearLayout.HORIZONTAL);
        onlineRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout onlineLeft = new LinearLayout(this);
        onlineLeft.setOrientation(LinearLayout.VERTICAL);
        TextView serverLabel = text("SERVIDOR OFICIAL", 11, 0xFFBDBDBD, true);
        statusText = text("● VERIFICANDO...", 15, 0xFFFF3B30, true);
        onlineLeft.addView(serverLabel);
        onlineLeft.addView(statusText);

        playersText = text("--/--", 22, Color.WHITE, true);
        playersText.setGravity(Gravity.END);
        TextView playersLabel = text("JOGADORES ONLINE", 9, 0xFF9B9B9B, true);
        playersLabel.setGravity(Gravity.END);
        LinearLayout onlineRight = new LinearLayout(this);
        onlineRight.setOrientation(LinearLayout.VERTICAL);
        onlineRight.setGravity(Gravity.END);
        onlineRight.addView(playersText);
        onlineRight.addView(playersLabel);

        onlineRow.addView(onlineLeft, new LinearLayout.LayoutParams(0, -2, 1f));
        onlineRow.addView(onlineRight, new LinearLayout.LayoutParams(-2, -2));
        onlineRow.setOnClickListener(v -> refreshServer());
        panel.addView(onlineRow, fullWrap());

        addGap(panel, 18);
        TextView nickLabel = text("SEU NICK", 10, 0xFFBDBDBD, true);
        panel.addView(nickLabel, fullWrap());
        addGap(panel, 6);

        nickInput = new EditText(this);
        nickInput.setSingleLine(true);
        nickInput.setTextColor(Color.WHITE);
        nickInput.setHintTextColor(0xFF686868);
        nickInput.setHint("Ex.: Junior_SGNT");
        nickInput.setTextSize(16);
        nickInput.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nickInput.setPadding(dp(16), 0, dp(16), 0);
        nickInput.setBackground(rounded(0xE6181818, 0xFF555555, 14));
        nickInput.setText(prefs.getString("nickname", ""));
        panel.addView(nickInput, new LinearLayout.LayoutParams(-1, dp(56)));

        addGap(panel, 14);
        Button play = new Button(this);
        play.setText("JOGAR");
        play.setTextColor(Color.WHITE);
        play.setTextSize(16);
        play.setTypeface(Typeface.DEFAULT_BOLD);
        play.setAllCaps(false);
        play.setGravity(Gravity.CENTER);
        play.setBackground(roundGradient(0xFFE10600, 0xFF9D0000, 16, 0xFFFF4A45));
        play.setOnClickListener(v -> play());
        panel.addView(play, new LinearLayout.LayoutParams(-1, dp(60)));

        addGap(panel, 12);
        TextView hint = text("Toque no status para atualizar", 10, 0xFF757575, false);
        hint.setGravity(Gravity.CENTER);
        panel.addView(hint, fullWrap());

        addGap(content, 18);
        LinearLayout socials = new LinearLayout(this);
        socials.setOrientation(LinearLayout.HORIZONTAL);
        socials.setGravity(Gravity.CENTER);
        socials.addView(social("INSTAGRAM", "https://instagram.com/gtasaogoncalo"), weighted());
        socials.addView(social("TIKTOK", "https://www.tiktok.com/@gtasaogoncalo"), weighted());
        socials.addView(social("YOUTUBE", "https://www.youtube.com/@gtasaogoncalo"), weighted());
        content.addView(socials, fullWrap());

        addGap(content, 12);
        TextView footer = text("@gtasaogoncalo  •  GTA SGNT RJ", 10, 0xFF777777, false);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer, fullWrap());

        return root;
    }

    private TextView social(String label, String url) {
        TextView t = text(label, 10, 0xFFD9D9D9, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(5), dp(10), dp(5), dp(10));
        t.setBackground(rounded(0x66151515, 0x44555555, 12));
        t.setOnClickListener(v -> openUrl(url));
        return t;
    }

    private void play() {
        String nick = nickInput.getText().toString().trim();
        if (!nick.matches("[A-Za-z0-9_]{3,24}")) {
            Toast.makeText(this, "Use um nick de 3 a 24 caracteres (letras, números e _).", Toast.LENGTH_LONG).show();
            return;
        }
        prefs.edit().putString("nickname", nick).apply();

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
                        "Cliente do GTA SGNT ainda será integrado ao APK.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshServer() {
        statusText.setText("● VERIFICANDO...");
        statusText.setTextColor(0xFFFF3B30);
        playersText.setText("--/--");
        executor.execute(() -> {
            try {
                ServerInfo info = queryServer(SERVER_IP, SERVER_PORT);
                runOnUiThread(() -> {
                    statusText.setText("● ONLINE");
                    statusText.setTextColor(0xFF35D46F);
                    playersText.setText(info.players + "/" + info.maxPlayers);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusText.setText("● OFFLINE");
                    statusText.setTextColor(0xFFFF3B30);
                    playersText.setText("0/0");
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
            socket.setSoTimeout(2200);
            socket.send(new DatagramPacket(request, request.length, addr, port));
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            int len = response.getLength();
            if (len < 17) throw new IllegalStateException("Resposta inválida");

            ByteBuffer bb = ByteBuffer.wrap(buffer, 11, len - 11).order(ByteOrder.LITTLE_ENDIAN);
            bb.get();
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

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable roundGradient(int top, int bottom, int radiusDp, int stroke) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams fullWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        p.setMargins(dp(4), 0, dp(4), 0);
        return p;
    }

    private void addGap(LinearLayout parent, int value) {
        Space s = new Space(this);
        parent.addView(s, new LinearLayout.LayoutParams(1, dp(value)));
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class ServerInfo {
        final int players;
        final int maxPlayers;
        final String hostname;
        ServerInfo(int players, int maxPlayers, String hostname) {
            this.players = players;
            this.maxPlayers = maxPlayers;
            this.hostname = hostname;
        }
    }
}
