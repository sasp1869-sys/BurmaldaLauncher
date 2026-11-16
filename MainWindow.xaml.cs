using Microsoft.Win32;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Windows;
using MessageBox = System.Windows.MessageBox;

namespace BurmaldaLauncher;

public partial class MainWindow : Window
{
    private const string ClientFileName = "BurmaldaClient.jar";
    private static readonly HttpClient Http = CreateHttpClient();
    private string? _gameDirectory;
    private readonly string _settingsDirectory =
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "BurmaldaLauncher");
    private string SettingsPath => Path.Combine(_settingsDirectory, "settings.json");

    public MainWindow()
    {
        InitializeComponent();
        LoadSettings();
        if (string.IsNullOrWhiteSpace(_gameDirectory))
        {
            var candidate = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
            if (Directory.Exists(candidate))
                _gameDirectory = candidate;
        }
        RefreshGamePath();
    }

    private static HttpClient CreateHttpClient()
    {
        var client = new HttpClient();
        client.DefaultRequestHeaders.UserAgent.Add(new ProductInfoHeaderValue("BurmaldaLauncher", "0.2.0"));
        client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/vnd.github+json"));
        return client;
    }

    private void LoadSettings()
    {
        try
        {
            if (!File.Exists(SettingsPath)) return;
            var settings = JsonSerializer.Deserialize<LauncherSettings>(File.ReadAllText(SettingsPath));
            _gameDirectory = settings?.GameDirectory;
            if (!string.IsNullOrWhiteSpace(settings?.ReleaseRepository))
                ReleaseRepoText.Text = settings.ReleaseRepository;
        }
        catch { /* A damaged settings file must not prevent the launcher from opening. */ }
    }

    private void SaveSettings()
    {
        try
        {
            Directory.CreateDirectory(_settingsDirectory);
            var settings = new LauncherSettings(_gameDirectory, ReleaseRepoText.Text.Trim());
            File.WriteAllText(SettingsPath, JsonSerializer.Serialize(settings, new JsonSerializerOptions { WriteIndented = true }));
        }
        catch (Exception ex)
        {
            StatusText.Text = "Не удалось сохранить настройки: " + ex.Message;
        }
    }

    private void RefreshGamePath()
    {
        GamePathText.Text = string.IsNullOrWhiteSpace(_gameDirectory)
            ? "Папка ещё не выбрана"
            : _gameDirectory;
    }

    private void ChooseFolder_Click(object sender, RoutedEventArgs e)
    {
        using var dialog = new System.Windows.Forms.FolderBrowserDialog
        {
            Description = "Выбери папку .minecraft",
            SelectedPath = _gameDirectory ?? ""
        };
        if (dialog.ShowDialog() == System.Windows.Forms.DialogResult.OK)
        {
            _gameDirectory = dialog.SelectedPath;
            RefreshGamePath();
            SaveSettings();
            StatusText.Text = "Папка игры выбрана";
        }
    }

    private void InstallJar_Click(object sender, RoutedEventArgs e)
    {
        if (!EnsureGameDirectory()) return;
        var dialog = new Microsoft.Win32.OpenFileDialog
        {
            Title = "Выбери JAR-файл Burmalda Client",
            Filter = "Java mod (*.jar)|*.jar",
            CheckFileExists = true
        };
        if (dialog.ShowDialog() != true) return;

        try
        {
            InstallJar(dialog.FileName);
            StatusText.Text = "Клиент установлен в папку mods";
            MessageBox.Show("Burmalda Client установлен. Если Minecraft был открыт, перезапусти его.", "Готово");
        }
        catch (Exception ex)
        {
            StatusText.Text = "Ошибка установки";
            MessageBox.Show("Не удалось установить клиент: " + ex.Message, "Ошибка");
        }
    }

    private async void UpdateClient_Click(object sender, RoutedEventArgs e)
    {
        if (!EnsureGameDirectory()) return;
        var repository = ReleaseRepoText.Text.Trim();
        var parts = repository.Split('/');
        if (parts.Length != 2 || parts.Any(string.IsNullOrWhiteSpace))
        {
            MessageBox.Show("Укажи репозиторий в формате владелец/репозиторий.", "Нужен репозиторий");
            return;
        }

        SetBusy(true, "Проверяю последний GitHub Release...");
        try
        {
            var apiUrl = $"https://api.github.com/repos/{parts[0]}/{parts[1]}/releases/latest";
            using var response = await Http.GetAsync(apiUrl);
            response.EnsureSuccessStatusCode();
            await using var stream = await response.Content.ReadAsStreamAsync();
            using var json = await JsonDocument.ParseAsync(stream);

            if (!json.RootElement.TryGetProperty("assets", out var assets))
                throw new InvalidDataException("В релизе нет списка файлов.");

            JsonElement? jarAsset = null;
            foreach (var asset in assets.EnumerateArray())
            {
                var name = asset.GetProperty("name").GetString() ?? "";
                if (name.EndsWith(".jar", StringComparison.OrdinalIgnoreCase) &&
                    (name.Contains("Burmalda", StringComparison.OrdinalIgnoreCase) || jarAsset is null))
                    jarAsset = asset;
                if (name.Equals(ClientFileName, StringComparison.OrdinalIgnoreCase))
                {
                    jarAsset = asset;
                    break;
                }
            }

            if (jarAsset is null)
                throw new FileNotFoundException("В последнем GitHub Release не найден JAR-файл.");

            var downloadUrl = jarAsset.Value.GetProperty("browser_download_url").GetString();
            if (string.IsNullOrWhiteSpace(downloadUrl))
                throw new InvalidDataException("У JAR-файла отсутствует ссылка для скачивания.");

            SetBusy(true, "Скачиваю обновление...");
            var bytes = await Http.GetByteArrayAsync(downloadUrl);
            if (bytes.Length < 1000)
                throw new InvalidDataException("Скачанный файл слишком маленький и не похож на JAR.");

            var tempFile = Path.Combine(Path.GetTempPath(), "BurmaldaClient-update.jar");
            await File.WriteAllBytesAsync(tempFile, bytes);
            InstallJar(tempFile);
            var tag = json.RootElement.TryGetProperty("tag_name", out var tagElement)
                ? tagElement.GetString() : "новая версия";
            StatusText.Text = $"Установлено обновление: {tag}";
            MessageBox.Show($"Burmalda Client обновлён ({tag}). Перезапусти Minecraft, если он был открыт.", "Обновление готово");
        }
        catch (HttpRequestException ex)
        {
            StatusText.Text = "Не удалось получить обновление";
            MessageBox.Show("GitHub не ответил или репозиторий недоступен. Проверь имя репозитория и интернет.\n\n" + ex.Message, "Ошибка обновления");
        }
        catch (Exception ex)
        {
            StatusText.Text = "Ошибка обновления";
            MessageBox.Show("Не удалось обновить клиент: " + ex.Message, "Ошибка");
        }
        finally
        {
            SetBusy(false, StatusText.Text);
            SaveSettings();
        }
    }

    private void InstallJar(string sourceFile)
    {
        if (!EnsureGameDirectory()) throw new DirectoryNotFoundException("Папка Minecraft не выбрана.");
        var mods = Path.Combine(_gameDirectory!, "mods");
        Directory.CreateDirectory(mods);
        var destination = Path.Combine(mods, ClientFileName);

        if (File.Exists(destination))
        {
            var backup = destination + ".backup-" + DateTime.Now.ToString("yyyyMMdd-HHmmss");
            File.Copy(destination, backup, false);
        }

        var temporaryDestination = destination + ".new";
        File.Copy(sourceFile, temporaryDestination, true);
        if (File.Exists(destination)) File.Delete(destination);
        File.Move(temporaryDestination, destination);
    }

    private bool EnsureGameDirectory()
    {
        if (!string.IsNullOrWhiteSpace(_gameDirectory) && Directory.Exists(_gameDirectory))
            return true;
        MessageBox.Show("Сначала выбери существующую папку .minecraft.", "Папка игры не выбрана");
        return false;
    }

    private void OpenMods_Click(object sender, RoutedEventArgs e)
    {
        if (!EnsureGameDirectory()) return;
        var mods = Path.Combine(_gameDirectory!, "mods");
        Directory.CreateDirectory(mods);
        Process.Start(new ProcessStartInfo("explorer.exe", mods) { UseShellExecute = true });
    }

    private void SetBusy(bool busy, string status)
    {
        StatusText.Text = status;
        Cursor = busy ? System.Windows.Input.Cursors.Wait : System.Windows.Input.Cursors.Arrow;
    }

    private sealed record LauncherSettings(string? GameDirectory, string? ReleaseRepository);
}