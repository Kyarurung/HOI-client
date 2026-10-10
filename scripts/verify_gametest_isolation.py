import argparse
import ctypes
import json
import os
from pathlib import Path
import subprocess
import tempfile


def alive(pid):
    if os.name == "nt":
        kernel = ctypes.WinDLL("kernel32", use_last_error=True)
        kernel.OpenProcess.restype = ctypes.c_void_p
        kernel.GetExitCodeProcess.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        kernel.CloseHandle.argtypes = [ctypes.c_void_p]
        handle = kernel.OpenProcess(0x1000, False, pid)
        if not handle:
            if ctypes.get_last_error() == 5:
                raise RuntimeError("Cannot inspect probe PID " + str(pid))
            return False
        try:
            code = ctypes.c_ulong()
            if not kernel.GetExitCodeProcess(handle, ctypes.byref(code)):
                raise ctypes.WinError()
            return code.value == 259
        finally:
            kernel.CloseHandle(handle)
    try:
        os.kill(pid, 0)
        return True
    except ProcessLookupError:
        return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    output = args.output.resolve() if args.output else Path(tempfile.mkdtemp(prefix="hoi-gametest-isolation-"))
    output.mkdir(parents=True, exist_ok=True)
    if any(output.iterdir()):
        raise ValueError("Use an empty output directory to preserve earlier evidence")
    wrapper = root / ("gradlew.bat" if os.name == "nt" else "gradlew")
    (output / "settings.gradle").write_text("rootProject.name = 'gametest-isolation-check'\n", encoding="utf-8")
    script = (root / "gradle/isolated-gametest.gradle").as_posix().replace("'", "\\'")
    (output / "build.gradle").write_text("""ext.hoiGameTestRunName = 'probe'
ext.hoiGameTestTaskName = 'probe'
tasks.register('probe', JavaExec) {
    mainClass = file('IsolationProbe.java').absolutePath
    args providers.gradleProperty('probeExit').orElse('0').get()
}
apply from: '""" + script + "'\n", encoding="utf-8")
    source = """import java.nio.channels.FileChannel;
import java.nio.file.*;

class IsolationProbe {
    public static void main(String[] args) throws Exception {
        var root = args[0].equals("hold") ? Path.of(args[1]) : Path.of(".");
        Files.createDirectories(root.resolve("logs"));
        Files.createDirectories(root.resolve("world"));
        if (args[0].equals("hold")) {
            try (var log = FileChannel.open(root.resolve("logs/latest.log"), StandardOpenOption.WRITE);
                 var world = FileChannel.open(root.resolve("world/session.lock"), StandardOpenOption.WRITE);
                 var first = log.lock(); var second = world.lock()) {
                System.out.println("LOCKED");
                System.out.flush();
                System.in.read();
            }
            return;
        }
        Files.writeString(root.resolve("child.pid"), Long.toString(ProcessHandle.current().pid()));
        Files.writeString(root.resolve("logs/latest.log"), "probe log");
        Files.writeString(root.resolve("world/session.lock"), "probe world");
        if (!args[0].equals("0")) throw new IllegalStateException("Intentional probe failure");
    }
}
"""
    (output / "IsolationProbe.java").write_text(source, encoding="utf-8")
    results = []

    def run(name, fail=False):
        command = [str(wrapper), "-p", str(output), "probe", "--console=plain", "--no-daemon", "--max-workers=1", "-PprobeExit=" + ("1" if fail else "0")]
        result = subprocess.run(command, cwd=root, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (output / (name + ".log")).write_bytes(result.stdout)
        assert (result.returncode != 0) == fail, name
        data = json.loads((output / "build/run/probe-latest.json").read_text(encoding="utf-8"))
        directory = Path(data["runDirectory"])
        assert directory.parent == output / "build/run/probe-runs"
        assert data["status"] == ("failed" if fail else "passed"), data
        assert all(row["runDirectory"] != str(directory) for row in results)
        pid = int((directory / "child.pid").read_text())
        assert not alive(pid), ("JavaExec left a child running", pid)
        results.append(dict(case=name, returncode=result.returncode, runDirectory=str(directory), childPid=pid, childExited=True, status=data["status"]))
        return directory

    first = run("single")
    before = {name: (first / name).read_bytes() for name in ["logs/latest.log", "world/session.lock", "hoi-run.json"]}
    java = str(Path(os.environ["JAVA_HOME"]) / "bin" / ("java.exe" if os.name == "nt" else "java"))
    holder = subprocess.Popen([java, str(output / "IsolationProbe.java"), "hold", str(first)], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        assert holder.stdout.readline().strip() == b"LOCKED", "Probe lock holder failed"
        run("previous-process-still-holds-files")
        assert holder.poll() is None
    finally:
        holder.communicate(input=b"\n", timeout=30)
    assert holder.returncode == 0
    assert before == {name: (first / name).read_bytes() for name in before}
    run("intentional-child-failure", fail=True)
    run("recovery-and-consecutive-rerun")
    (output / "results.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    print("Passed four isolation/lifecycle checks; evidence:", output)


if __name__ == "__main__":
    main()
