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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final String SERVER_IP = "51.222.193.109";
    private static final int SERVER_PORT = 7777;

    // Assim que houver um link DIRETO .zip da DATA, cole aqui. Enquanto estiver vazio,
    // a V3 usa o pacote bootstrap incluído no APK apenas para testar o fluxo de preparação.
    private static final String DATA_URL = "";
    private static final int DATA_VERSION = 1;

    // Clientes suportados para o teste de login. O launcher tenta na ordem abaixo.
    // Quando o cliente nativo for incorporado ao mesmo pacote, esta ponte deixa de ser necessária.
    private static final String[][] GAME_CLIENTS = new String[][] {
            {"ro.alyn_sampmobile.game", "ro.alyn_sampmobile.game.SAMP"},
            {"com.rockstargames.gtasa", "com.rockstargames.gtasa.GTASA"},
            {"com.eaglevision.samp", "com.eaglevision.samp.launcher.activity.CheckActivity"}
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private TextView serverStatusText, playersText, gameStatusText, progressText;
    private ProgressBar progressBar;
    private EditText nickInput;
    private Button actionButton;
    private volatile boolean preparing = false;

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
        refreshGameState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serverStatusText != null) refreshServer();
        if (gameStatusText != null && !preparing) refreshGameState();
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ImageView background = new ImageView(this);
        background.setImageResource(R.drawable.sgnt_background);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(background, new FrameLayout.LayoutParams(-1, -1));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x22000000, 0x70000000, 0xF7000000}));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(22), dp(40), dp(22), dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -1));

        Space hero = new Space(this);
        content.addView(hero, new LinearLayout.LayoutParams(1, 0, 1f));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(roundGradient(0xE0131313, 0xF8080808, 22, 0x55E10600));
        content.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout onlineRow = new LinearLayout(this);
        onlineRow.setOrientation(LinearLayout.HORIZONTAL);
        onlineRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout onlineLeft = new LinearLayout(this);
        onlineLeft.setOrientation(LinearLayout.VERTICAL);
        onlineLeft.addView(text("SERVIDOR OFICIAL", 11, 0xFFBDBDBD, true));
        serverStatusText = text("● VERIFICANDO...", 15, 0xFFFF3B30, true);
        onlineLeft.addView(serverStatusText);
        LinearLayout onlineRight = new LinearLayout(this);
        onlineRight.setOrientation(LinearLayout.VERTICAL);
        onlineRight.setGravity(Gravity.END);
        playersText = text("--/--", 22, Color.WHITE, true);
        playersText.setGravity(Gravity.END);
        TextView playersLabel = text("JOGADORES ONLINE", 9, 0xFF9B9B9B, true);
        playersLabel.setGravity(Gravity.END);
        onlineRight.addView(playersText);
        onlineRight.addView(playersLabel);
        onlineRow.addView(onlineLeft, new LinearLayout.LayoutParams(0, -2, 1f));
        onlineRow.addView(onlineRight, new LinearLayout.LayoutParams(-2, -2));
        onlineRow.setOnClickListener(v -> refreshServer());
        panel.addView(onlineRow, fullWrap());

        addGap(panel, 16);
        View divider = new View(this);
        divider.setBackgroundColor(0x33FFFFFF);
        panel.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        addGap(panel, 16);

        TextView gameLabel = text("STATUS DO JOGO", 10, 0xFFBDBDBD, true);
        panel.addView(gameLabel, fullWrap());
        gameStatusText = text("VERIFICANDO...", 16, 0xFFFFC107, true);
        panel.addView(gameStatusText, fullWrap());
        addGap(panel, 8);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        panel.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(8)));
        progressText = text("", 10, 0xFFAAAAAA, false);
        progressText.setVisibility(View.GONE);
        panel.addView(progressText, fullWrap());

        addGap(panel, 14);
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
        actionButton = new Button(this);
        actionButton.setText("VERIFICANDO...");
        actionButton.setTextColor(Color.WHITE);
        actionButton.setTextSize(16);
        actionButton.setTypeface(Typeface.DEFAULT_BOLD);
        actionButton.setAllCaps(false);
        actionButton.setGravity(Gravity.CENTER);
        actionButton.setBackground(roundGradient(0xFFE10600, 0xFF9D0000, 16, 0xFFFF4A45));
        actionButton.setEnabled(false);
        actionButton.setOnClickListener(v -> onMainAction());
        panel.addView(actionButton, new LinearLayout.LayoutParams(-1, dp(60)));

        addGap(panel, 10);
        TextView hint = text("A DATA é verificada automaticamente ao abrir", 10, 0xFF757575, false);
        hint.setGravity(Gravity.CENTER);
        panel.addView(hint, fullWrap());

        addGap(content, 14);
        TextView footer = text("@gtasaogoncalo  •  GTA SGNT RJ", 10, 0xFF777777, false);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer, fullWrap());
        return root;
    }

    private void onMainAction() {
        if (preparing) return;
        if (!isGameReady()) prepareGame();
        else play();
    }

    private File gameDir() {
        File base = getExternalFilesDir(null);
        if (base == null) base = getFilesDir();
        return new File(base, "GTA_SGNT_RJ");
    }

    private File markerFile() { return new File(gameDir(), ".sgnt_data_v" + DATA_VERSION); }

    private boolean isGameReady() {
        File dir = gameDir();
        File marker = markerFile();
        return dir.isDirectory() && marker.isFile() && dir.list() != null && dir.list().length > 1;
    }

    private void refreshGameState() {
        boolean ready = isGameReady();
        if (ready) {
            gameStatusText.setText("PRONTO PARA JOGAR ✓");
            gameStatusText.setTextColor(0xFF35D46F);
            actionButton.setText("JOGAR");
        } else {
            gameStatusText.setText("NÃO PRONTO");
            gameStatusText.setTextColor(0xFFFF5252);
            actionButton.setText("PREPARAR JOGO");
        }
        actionButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
    }

    private void prepareGame() {
        preparing = true;
        actionButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        gameStatusText.setText("PREPARANDO...");
        gameStatusText.setTextColor(0xFFFFC107);
        executor.execute(() -> {
            File zip = new File(getCacheDir(), "sgnt_data.zip");
            try {
                if (DATA_URL != null && DATA_URL.startsWith("http")) {
                    setProgressUi("Baixando DATA...", 0);
                    download(DATA_URL, zip);
                } else {
                    setProgressUi("Carregando pacote de preparação...", 5);
                    copyAsset("sgnt_bootstrap_data.zip", zip);
                    setProgressUi("Pacote carregado", 35);
                }
                File dest = gameDir();
                if (!dest.exists() && !dest.mkdirs()) throw new Exception("Não foi possível criar a pasta da DATA");
                setProgressUi("Extraindo arquivos...", 40);
                unzip(zip, dest);
                setProgressUi("Verificando arquivos...", 95);
                try (FileOutputStream fos = new FileOutputStream(markerFile())) {
                    fos.write(("GTA SGNT DATA " + DATA_VERSION).getBytes(StandardCharsets.UTF_8));
                }
                setProgressUi("Concluído", 100);
                Thread.sleep(350);
                runOnUiThread(() -> {
                    preparing = false;
                    gameStatusText.setText("PRONTO PARA JOGAR ✓");
                    gameStatusText.setTextColor(0xFF35D46F);
                    actionButton.setText("JOGAR");
                    actionButton.setEnabled(true);
                    progressText.setText("DATA preparada com sucesso");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    preparing = false;
                    gameStatusText.setText("NÃO PRONTO");
                    gameStatusText.setTextColor(0xFFFF5252);
                    actionButton.setText("TENTAR NOVAMENTE");
                    actionButton.setEnabled(true);
                    progressText.setText("Erro: " + e.getMessage());
                    Toast.makeText(this, "Falha ao preparar a DATA.", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (zip.exists()) zip.delete();
            }
        });
    }

    private void download(String urlString, File outFile) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlString).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "GTA-SGNT-RJ-Launcher/1.0.0");
        c.connect();
        if (c.getResponseCode() < 200 || c.getResponseCode() >= 300) throw new Exception("HTTP " + c.getResponseCode());
        long total = c.getContentLengthLong();
        try (InputStream in = new BufferedInputStream(c.getInputStream());
             FileOutputStream fos = new FileOutputStream(outFile);
             BufferedOutputStream out = new BufferedOutputStream(fos)) {
            byte[] buf = new byte[64 * 1024];
            long done = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                done += n;
                int pct = total > 0 ? (int)Math.min(70, (done * 70L / total)) : 25;
                setProgressUi(total > 0 ? "Baixando DATA... " + (done / 1024 / 1024) + " / " + (total / 1024 / 1024) + " MB" : "Baixando DATA...", pct);
            }
        } finally { c.disconnect(); }
    }

    private void copyAsset(String name, File outFile) throws Exception {
        try (InputStream in = getAssets().open(name); FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] b = new byte[64 * 1024]; int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
        }
    }

    private void unzip(File zipFile, File destination) throws Exception {
        long zipSize = Math.max(1, zipFile.length());
        long approxDone = 0;
        String destPath = destination.getCanonicalPath() + File.separator;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(destination, entry.getName());
                String canonical = out.getCanonicalPath();
                if (!canonical.startsWith(destPath)) throw new SecurityException("ZIP inválido");
                if (entry.isDirectory()) {
                    if (!out.exists()) out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        int n;
                        while ((n = zis.read(buffer)) != -1) {
                            bos.write(buffer, 0, n);
                            approxDone += n;
                            int pct = 70 + (int)Math.min(24, approxDone * 24L / Math.max(zipSize, approxDone));
                            setProgressUi("Extraindo: " + entry.getName(), pct);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void setProgressUi(String msg, int pct) {
        runOnUiThread(() -> { progressText.setText(msg); progressBar.setProgress(Math.max(0, Math.min(100, pct))); });
    }

    private void play() {
        String nick = nickInput.getText().toString().trim();
        if (!nick.matches("[A-Za-z0-9_]{3,24}")) {
            Toast.makeText(this, "Use um nick de 3 a 24 caracteres (letras, números e _).", Toast.LENGTH_LONG).show();
            return;
        }
        if (!isGameReady()) { refreshGameState(); return; }
        prefs.edit().putString("nickname", nick).apply();
        // Tenta abrir diretamente um cliente compatível já instalado no aparelho.
        // Enviamos várias chaves de extras porque launchers/clientes diferentes usam nomes diferentes.
        for (String[] client : GAME_CLIENTS) {
            String pkg = client[0];
            String activity = client[1];
            try {
                Intent game = new Intent(Intent.ACTION_VIEW);
                game.setComponent(new ComponentName(pkg, activity));
                game.putExtra("nickname", nick);
                game.putExtra("nick", nick);
                game.putExtra("player_name", nick);
                game.putExtra("server_ip", SERVER_IP);
                game.putExtra("ip", SERVER_IP);
                game.putExtra("host", SERVER_IP);
                game.putExtra("server_port", SERVER_PORT);
                game.putExtra("port", SERVER_PORT);
                game.setData(Uri.parse("samp://" + SERVER_IP + ":" + SERVER_PORT));
                startActivity(game);
                return;
            } catch (Exception ignored) {
                try {
                    Intent fallback = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (fallback != null) {
                        fallback.putExtra("nickname", nick);
                        fallback.putExtra("nick", nick);
                        fallback.putExtra("server_ip", SERVER_IP);
                        fallback.putExtra("server_port", SERVER_PORT);
                        fallback.setData(Uri.parse("samp://" + SERVER_IP + ":" + SERVER_PORT));
                        startActivity(fallback);
                        return;
                    }
                } catch (Exception ignored2) { }
            }
        }

        Toast.makeText(this,
                "DATA pronta, mas nenhum cliente GTA/SA:MP compatível foi encontrado no aparelho.",
                Toast.LENGTH_LONG).show();
    }

    private void refreshServer() {
        serverStatusText.setText("● VERIFICANDO...");
        serverStatusText.setTextColor(0xFFFFC107); playersText.setText("--/--");
        executor.execute(() -> {
            Exception last = null;
            for (int i=0; i<3; i++) {
                try {
                    ServerInfo info = queryServer(SERVER_IP, SERVER_PORT);
                    runOnUiThread(() -> { serverStatusText.setText("● ONLINE"); serverStatusText.setTextColor(0xFF35D46F); playersText.setText(info.players + "/" + info.maxPlayers); });
                    return;
                } catch (Exception e) { last = e; try { Thread.sleep(350); } catch (InterruptedException ignored) {} }
            }
            runOnUiThread(() -> { serverStatusText.setText("● OFFLINE"); serverStatusText.setTextColor(0xFFFF3B30); playersText.setText("0/0"); });
        });
    }

    private ServerInfo queryServer(String ip, int port) throws Exception {
        InetAddress addr = InetAddress.getByName(ip); byte[] ipBytes = addr.getAddress();
        ByteArrayOutputStream out = new ByteArrayOutputStream(); out.write(new byte[]{'S','A','M','P'}); out.write(ipBytes); out.write(port & 0xFF); out.write((port >> 8) & 0xFF); out.write('i');
        byte[] req = out.toByteArray();
        DatagramSocket socket = new DatagramSocket(); socket.setSoTimeout(1500); socket.send(new DatagramPacket(req, req.length, addr, port));
        byte[] buf = new byte[4096]; DatagramPacket response = new DatagramPacket(buf, buf.length); socket.receive(response); socket.close();
        if (response.getLength() < 16) throw new Exception("Resposta inválida");
        byte[] data = response.getData();
        if (data[0] != 'S' || data[1] != 'A' || data[2] != 'M' || data[3] != 'P' || data[10] != 'i')
            throw new Exception("Resposta inválida");
        ByteBuffer bb = ByteBuffer.wrap(data, 11, response.getLength() - 11).order(ByteOrder.LITTLE_ENDIAN);
        bb.get(); // passworded: 0 ou 1
        int players = bb.getShort() & 0xFFFF;
        int maxPlayers = bb.getShort() & 0xFFFF;
        return new ServerInfo(players, maxPlayers);
    }

    private static class ServerInfo { final int players, maxPlayers; ServerInfo(int p, int m){players=p;maxPlayers=m;} }
    private TextView text(String s,int sp,int color,boolean bold){ TextView t=new TextView(this); t.setText(s);t.setTextSize(sp);t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT_BOLD); return t; }
    private LinearLayout.LayoutParams fullWrap(){return new LinearLayout.LayoutParams(-1,-2);} private LinearLayout.LayoutParams weighted(){return new LinearLayout.LayoutParams(0,-2,1f);} private void addGap(LinearLayout p,int d){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,dp(d)));}
    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private GradientDrawable roundGradient(int top,int bottom,int radius,int stroke){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}    
}
