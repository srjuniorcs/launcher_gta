package br.com.gtasgntrj.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
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

import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final String SERVER_IP = "51.222.193.109";
    private static final int SERVER_PORT = 7777;
    private static final String GAME_PACKAGE = "com.rockstargames.gtasa";
    private static final int DATA_VERSION = 1;

    // Link publico da DATA que voce ja tinha no MediaFire. O launcher resolve o link direto automaticamente.
    private static final String DATA_PAGE_URL = "https://www.mediafire.com/file/462u64oylkt5eqz/Data_Sem_Mods_Samp_Alyn_Todas_Gpus.zip/file";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private TextView serverStatusText, playersText, gameStatusText, progressText;
    private ProgressBar progressBar;
    private EditText nickInput;
    private Button actionButton;
    private volatile boolean preparing = false;
    private boolean pendingPrepareAfterPermission = false;
    private boolean pendingPrepareAfterInstall = false;

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
        if (pendingPrepareAfterPermission && hasFileAccess()) {
            pendingPrepareAfterPermission = false;
            startPrepareFlow();
            return;
        }
        if (pendingPrepareAfterInstall && isClientInstalled()) {
            pendingPrepareAfterInstall = false;
            prepareData();
            return;
        }
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
        serverStatusText = text("● VERIFICANDO...", 15, 0xFFFFC107, true);
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

        panel.addView(text("STATUS DO JOGO", 10, 0xFFBDBDBD, true), fullWrap());
        gameStatusText = text("VERIFICANDO...", 16, 0xFFFFC107, true);
        panel.addView(gameStatusText, fullWrap());
        addGap(panel, 8);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        panel.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(8)));
        progressText = text("", 10, 0xFFBDBDBD, false);
        progressText.setVisibility(View.GONE);
        panel.addView(progressText, fullWrap());

        addGap(panel, 14);
        panel.addView(text("SEU NICK", 10, 0xFFBDBDBD, true), fullWrap());
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
        TextView hint = text("Primeira abertura: o launcher prepara o cliente e a DATA automaticamente", 10, 0xFF757575, false);
        hint.setGravity(Gravity.CENTER);
        panel.addView(hint, fullWrap());

        addGap(content, 14);
        TextView footer = text("GTA SGNT RJ  •  v1.0.0  •  @gtasaogoncalo", 10, 0xFF777777, false);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer, fullWrap());
        return root;
    }

    private void onMainAction() {
        if (preparing) return;
        String nick = getValidNick();
        if (nick == null) return;
        prefs.edit().putString("nickname", nick).apply();
        if (isGameReady()) play(); else startPrepareFlow();
    }

    private void startPrepareFlow() {
        if (preparing) return;
        String nick = getValidNick();
        if (nick == null) return;
        prefs.edit().putString("nickname", nick).apply();

        if (!hasFileAccess()) {
            requestFileAccess();
            return;
        }
        if (!isClientInstalled()) {
            installEmbeddedClient();
            return;
        }
        prepareData();
    }

    private String getValidNick() {
        String nick = nickInput.getText().toString().trim();
        if (!nick.matches("[A-Za-z0-9_]{3,24}")) {
            Toast.makeText(this, "Use um nick de 3 a 24 caracteres (letras, números e _).", Toast.LENGTH_LONG).show();
            return null;
        }
        return nick;
    }

    private boolean hasFileAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return Environment.isExternalStorageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestFileAccess() {
        pendingPrepareAfterPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
            Toast.makeText(this, "Ative 'Permitir acesso a todos os arquivos' e volte ao GTA SGNT RJ.", Toast.LENGTH_LONG).show();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            pendingPrepareAfterPermission = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startPrepareFlow();
            else Toast.makeText(this, "Sem acesso aos arquivos não é possível preparar a DATA.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isClientInstalled() {
        try {
            getPackageManager().getPackageInfo(GAME_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void installEmbeddedClient() {
        preparing = true;
        actionButton.setEnabled(false);
        gameStatusText.setText("INSTALANDO CLIENTE...");
        gameStatusText.setTextColor(0xFFFFC107);
        showProgress("Preparando componente do jogo...", 15);
        executor.execute(() -> {
            try {
                File apk = new File(getCacheDir(), "GTA_SGNT_CLIENT.apk");
                copyAsset("GTA_SGNT_CLIENT.apk", apk);
                runOnUiThread(() -> {
                    try {
                        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
                        Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        install.setData(uri);
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        install.putExtra(Intent.EXTRA_RETURN_RESULT, false);
                        pendingPrepareAfterInstall = true;
                        preparing = false;
                        progressText.setText("Confirme a instalação do componente GTA SGNT RJ e volte.");
                        actionButton.setText("AGUARDANDO INSTALAÇÃO...");
                        startActivity(install);
                    } catch (Exception e) {
                        preparing = false;
                        showError("Não foi possível abrir o instalador do cliente: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    preparing = false;
                    showError("Falha ao preparar cliente: " + e.getMessage());
                });
            }
        });
    }

    private File androidDataRoot() {
        return new File(Environment.getExternalStorageDirectory(), "Android/data");
    }

    private File gamePackageDir() {
        return new File(androidDataRoot(), GAME_PACKAGE);
    }

    private File sampDir() {
        return new File(gamePackageDir(), "files/SAMP");
    }

    private File markerFile() {
        return new File(sampDir(), ".sgnt_data_v" + DATA_VERSION);
    }

    private boolean hasFullGameData() {
        File gta3 = new File(gamePackageDir(), "files/texdb/gta3");
        File files = new File(gamePackageDir(), "files");
        return gta3.isDirectory() || (files.isDirectory() && folderSizeAtLeast(files, 100L * 1024L * 1024L));
    }

    private boolean folderSizeAtLeast(File dir, long threshold) {
        if (dir == null || !dir.exists()) return false;
        long[] total = {0L};
        addSize(dir, total, threshold);
        return total[0] >= threshold;
    }

    private void addSize(File f, long[] total, long threshold) {
        if (total[0] >= threshold || f == null || !f.exists()) return;
        if (f.isFile()) { total[0] += f.length(); return; }
        File[] list = f.listFiles();
        if (list != null) for (File c : list) { addSize(c, total, threshold); if (total[0] >= threshold) return; }
    }

    private boolean isGameReady() {
        return isClientInstalled() && markerFile().isFile() && hasFullGameData();
    }

    private void refreshGameState() {
        boolean installed = isClientInstalled();
        boolean ready = isGameReady();
        if (ready) {
            gameStatusText.setText("PRONTO PARA JOGAR ✓");
            gameStatusText.setTextColor(0xFF35D46F);
            actionButton.setText("JOGAR");
        } else if (installed) {
            gameStatusText.setText("NÃO PRONTO • DATA PENDENTE");
            gameStatusText.setTextColor(0xFFFF5252);
            actionButton.setText("PREPARAR JOGO");
        } else {
            gameStatusText.setText("NÃO PRONTO");
            gameStatusText.setTextColor(0xFFFF5252);
            actionButton.setText("PREPARAR JOGO");
        }
        actionButton.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
    }

    private void prepareData() {
        if (preparing) return;
        preparing = true;
        actionButton.setEnabled(false);
        gameStatusText.setText("PREPARANDO...");
        gameStatusText.setTextColor(0xFFFFC107);
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            File download = new File(getExternalCacheDir() != null ? getExternalCacheDir() : getCacheDir(), "sgnt_data_full.zip");
            try {
                // 1) Base SAMP inclusa no instalador.
                setProgressUi("Extraindo base do SA:MP...", 3);
                File baseZip = new File(getCacheDir(), "sgnt_samp_cache.zip");
                copyAsset("sgnt_samp_cache.zip", baseZip);
                unzipFlexible(baseZip, androidDataRoot(), 3, 8);
                baseZip.delete();

                // 2) DATA completa do usuário via MediaFire.
                setProgressUi("Localizando download da DATA...", 9);
                String direct = resolveMediaFireDownload(DATA_PAGE_URL);
                setProgressUi("Baixando DATA...", 10);
                downloadFile(direct, download, 10, 72);

                setProgressUi("Extraindo DATA...", 73);
                unzipFlexible(download, androidDataRoot(), 73, 96);

                setProgressUi("Salvando configuração do servidor...", 97);
                writeSettings(prefs.getString("nickname", "Jogador_SGNT"));
                File marker = markerFile();
                if (!marker.getParentFile().exists()) marker.getParentFile().mkdirs();
                try (FileOutputStream out = new FileOutputStream(marker)) {
                    out.write(("GTA SGNT RJ DATA v" + DATA_VERSION).getBytes(StandardCharsets.UTF_8));
                }

                if (!hasFullGameData()) throw new Exception("A DATA foi extraída, mas os arquivos principais do GTA não foram encontrados.");
                setProgressUi("Jogo pronto!", 100);
                runOnUiThread(() -> {
                    preparing = false;
                    gameStatusText.setText("PRONTO PARA JOGAR ✓");
                    gameStatusText.setTextColor(0xFF35D46F);
                    actionButton.setText("JOGAR");
                    actionButton.setEnabled(true);
                    progressText.setText("Tudo pronto. Clique em JOGAR.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    preparing = false;
                    showError("Falha ao preparar: " + e.getMessage());
                });
            } finally {
                if (download.exists()) download.delete();
            }
        });
    }

    private String resolveMediaFireDownload(String page) throws Exception {
        HttpURLConnection c = open(page);
        String html;
        try (InputStream in = new BufferedInputStream(c.getInputStream())) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[32 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            html = out.toString("UTF-8");
        } finally { c.disconnect(); }
        html = html.replace("&amp;", "&").replace("\\/", "/");
        Pattern[] patterns = new Pattern[]{
                Pattern.compile("href=[\\\"'](https?://download[^\\\"']*mediafire\\.com/[^\\\"']+)[\\\"']", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(https?://download[^\\\"'\\s]+mediafire\\.com/[^\\\"'\\s]+)", Pattern.CASE_INSENSITIVE)
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(html);
            if (m.find()) return m.group(1);
        }
        throw new Exception("MediaFire não retornou o link direto. Tente novamente.");
    }

    private HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(45000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36 GTA-SGNT-RJ/1.0.0");
        c.setRequestProperty("Accept", "*/*");
        c.connect();
        int code = c.getResponseCode();
        if (code < 200 || code >= 400) throw new Exception("HTTP " + code);
        return c;
    }

    private void downloadFile(String url, File outFile, int startPct, int endPct) throws Exception {
        HttpURLConnection c = open(url);
        long total = c.getContentLengthLong();
        try (InputStream in = new BufferedInputStream(c.getInputStream());
             OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
            byte[] buf = new byte[128 * 1024];
            long done = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                done += n;
                int pct = startPct;
                if (total > 0) pct = startPct + (int)Math.min(endPct - startPct, done * (endPct - startPct) / total);
                String msg = total > 0
                        ? String.format(Locale.US, "Baixando DATA... %d / %d MB", done / 1048576, total / 1048576)
                        : String.format(Locale.US, "Baixando DATA... %d MB", done / 1048576);
                setProgressUi(msg, pct);
            }
        } finally { c.disconnect(); }
    }

    private void unzipFlexible(File zipFile, File androidData, int startPct, int endPct) throws Exception {
        long compressedSize = Math.max(1L, zipFile.length());
        long written = 0L;
        String rootCanonical = androidData.getCanonicalPath() + File.separator;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[128 * 1024];
            while ((entry = zis.getNextEntry()) != null) {
                String name = sanitizeEntry(entry.getName());
                if (name.length() == 0) { zis.closeEntry(); continue; }
                File out = chooseDataDestination(androidData, name);
                String canonical = out.getCanonicalPath();
                if (!canonical.startsWith(rootCanonical)) throw new SecurityException("ZIP inválido");
                if (entry.isDirectory()) {
                    if (!out.exists()) out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) throw new Exception("Não foi possível criar " + parent.getName());
                    try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(out))) {
                        int n;
                        while ((n = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, n);
                            written += n;
                            int pct = startPct + (int)Math.min(endPct - startPct,
                                    written * (endPct - startPct) / Math.max(compressedSize, written));
                            setProgressUi("Extraindo: " + out.getName(), pct);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private String sanitizeEntry(String name) {
        if (name == null) return "";
        name = name.replace('\\', '/');
        while (name.startsWith("/")) name = name.substring(1);
        // Remove prefixos comuns de ZIP de DATA.
        String lower = name.toLowerCase(Locale.US);
        int idx = lower.indexOf("android/data/");
        if (idx >= 0) name = name.substring(idx + "android/data/".length());
        return name;
    }

    private File chooseDataDestination(File androidData, String name) {
        String n = name;
        String lower = n.toLowerCase(Locale.US);
        if (lower.startsWith(GAME_PACKAGE.toLowerCase(Locale.US) + "/")) return new File(androidData, n);
        if (lower.startsWith("files/") || lower.startsWith("cache/")) return new File(gamePackageDir(), n);
        // Se o ZIP vier com uma pasta externa (ex.: Data_Sem_Mods/.../com.rockstargames.gtasa), recorta no pacote.
        int pkg = lower.indexOf(GAME_PACKAGE.toLowerCase(Locale.US) + "/");
        if (pkg >= 0) return new File(androidData, n.substring(pkg));
        return new File(gamePackageDir(), n);
    }

    private void writeSettings(String nick) throws Exception {
        File dir = sampDir();
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Não foi possível criar a pasta SAMP");
        File f = new File(dir, "settings.ini");
        String content = "[client]\n" +
                "name = " + nick + "\n" +
                "host = " + SERVER_IP + "\n" +
                "port = " + SERVER_PORT + "\n" +
                "password = \n\n" +
                "[debug]\n" +
                "debug = false\n" +
                "online = true\n\n" +
                "[gui]\n" +
                "Font = Arial.ttf\n" +
                "FontSize = 30.0\n" +
                "FontOutline = 2\n" +
                "ChatPosX = 325.0\n" +
                "ChatPosY = 25.0\n" +
                "ChatSizeX = 1150.0\n" +
                "ChatSizeY = 220.0\n" +
                "ChatMaxMessages = 8\n" +
                "HealthBarWidth = 60.0\n" +
                "HealthBarHeight = 10.0\n";
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void play() {
        String nick = getValidNick();
        if (nick == null) return;
        prefs.edit().putString("nickname", nick).apply();
        if (!isGameReady()) { refreshGameState(); return; }
        try {
            writeSettings(nick);
            Intent game = getPackageManager().getLaunchIntentForPackage(GAME_PACKAGE);
            if (game == null) throw new Exception("Cliente não encontrado");
            game.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(game);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível iniciar o GTA SGNT RJ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void copyAsset(String name, File outFile) throws Exception {
        try (InputStream in = getAssets().open(name); OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
            byte[] b = new byte[128 * 1024];
            int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
        }
    }

    private void showProgress(String msg, int pct) {
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        progressBar.setProgress(pct);
        progressText.setText(msg);
    }

    private void setProgressUi(String msg, int pct) {
        runOnUiThread(() -> showProgress(msg, Math.max(0, Math.min(100, pct))));
    }

    private void showError(String msg) {
        gameStatusText.setText("NÃO PRONTO");
        gameStatusText.setTextColor(0xFFFF5252);
        actionButton.setText("TENTAR NOVAMENTE");
        actionButton.setEnabled(true);
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        progressText.setText(msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void refreshServer() {
        serverStatusText.setText("● VERIFICANDO...");
        serverStatusText.setTextColor(0xFFFFC107);
        playersText.setText("--/--");
        executor.execute(() -> {
            for (int i = 0; i < 3; i++) {
                try {
                    ServerInfo info = queryServer(SERVER_IP, SERVER_PORT);
                    runOnUiThread(() -> {
                        serverStatusText.setText("● ONLINE");
                        serverStatusText.setTextColor(0xFF35D46F);
                        playersText.setText(info.players + "/" + info.maxPlayers);
                    });
                    return;
                } catch (Exception ignored) {
                    try { Thread.sleep(350); } catch (InterruptedException ignored2) { }
                }
            }
            runOnUiThread(() -> {
                serverStatusText.setText("● OFFLINE");
                serverStatusText.setTextColor(0xFFFF3B30);
                playersText.setText("0/0");
            });
        });
    }

    private ServerInfo queryServer(String ip, int port) throws Exception {
        InetAddress addr = InetAddress.getByName(ip);
        byte[] ipBytes = addr.getAddress();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{'S','A','M','P'});
        out.write(ipBytes);
        out.write(port & 0xFF);
        out.write((port >> 8) & 0xFF);
        out.write('i');
        byte[] req = out.toByteArray();
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(1700);
        socket.send(new DatagramPacket(req, req.length, addr, port));
        byte[] buf = new byte[4096];
        DatagramPacket response = new DatagramPacket(buf, buf.length);
        socket.receive(response);
        socket.close();
        if (response.getLength() < 16) throw new Exception("Resposta inválida");
        byte[] data = response.getData();
        if (data[0] != 'S' || data[1] != 'A' || data[2] != 'M' || data[3] != 'P' || data[10] != 'i') throw new Exception("Resposta inválida");
        ByteBuffer bb = ByteBuffer.wrap(data, 11, response.getLength() - 11).order(ByteOrder.LITTLE_ENDIAN);
        bb.get();
        int players = bb.getShort() & 0xFFFF;
        int maxPlayers = bb.getShort() & 0xFFFF;
        return new ServerInfo(players, maxPlayers);
    }

    private static class ServerInfo {
        final int players, maxPlayers;
        ServerInfo(int p, int m) { players = p; maxPlayers = m; }
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }
    private LinearLayout.LayoutParams fullWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private void addGap(LinearLayout p, int d) { Space s = new Space(this); p.addView(s, new LinearLayout.LayoutParams(1, dp(d))); }
    private GradientDrawable rounded(int fill, int stroke, int radius) { GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(radius)); g.setStroke(dp(1), stroke); return g; }
    private GradientDrawable roundGradient(int top, int bottom, int radius, int stroke) { GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom}); g.setCornerRadius(dp(radius)); g.setStroke(dp(1), stroke); return g; }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
