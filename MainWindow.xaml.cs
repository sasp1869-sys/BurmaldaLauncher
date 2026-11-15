
using Microsoft.Win32;
using System.IO;
using System.Windows;
using MessageBox = System.Windows.MessageBox;

namespace BurmaldaLauncher;

public partial class MainWindow : Window
{
    private string? _gameDirectory;

    public MainWindow()
    {
        InitializeComponent();

        var candidates = new[]
        {
            Path.Combine(
                Environment.GetFolderPath(
                    Environment.SpecialFolder.ApplicationData),
                ".minecraft")
        };

        _gameDirectory = candidates.FirstOrDefault(Directory.Exists);

        if (_gameDirectory is not null)
            GamePathText.Text = _gameDirectory;
    }

    private void ChooseFolder_Click(object sender, RoutedEventArgs e)
    {
        using var dialog = new System.Windows.Forms.FolderBrowserDialog
        {
            Description = "Выбери папку .minecraft"
        };

        if (dialog.ShowDialog() == System.Windows.Forms.DialogResult.OK)
        {
            _gameDirectory = dialog.SelectedPath;
            GamePathText.Text = _gameDirectory;
            StatusText.Text = "Папка игры выбрана";
        }
    }

    private void InstallJar_Click(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrWhiteSpace(_gameDirectory) ||
            !Directory.Exists(_gameDirectory))
        {
            MessageBox.Show(
                "Сначала выбери существующую папку Minecraft.",
                "Burmalda Launcher");
            return;
        }

        var dialog = new Microsoft.Win32.OpenFileDialog
        {
            Title = "Выбери JAR-файл Burmalda Client",
            Filter = "Java mod (*.jar)|*.jar",
            CheckFileExists = true
        };

        if (dialog.ShowDialog() != true)
            return;

        try
        {
            var mods = Path.Combine(_gameDirectory, "mods");
            Directory.CreateDirectory(mods);

            var destination = Path.Combine(
                mods, "BurmaldaClient.jar");

            File.Copy(dialog.FileName, destination, true);

            StatusText.Text = "Клиент установлен в mods";

            MessageBox.Show(
                "Файл клиента скопирован!",
                "Готово");
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                "Ошибка установки: " + ex.Message,
                "Ошибка");
        }
    }
}
