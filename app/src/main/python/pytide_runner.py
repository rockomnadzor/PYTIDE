import sys
import traceback


class _KotlinStdout:
    def __init__(self, sink):
        self.sink = sink

    def write(self, s):
        if s:
            self.sink.write(s)
        return len(s)

    def flush(self):
        pass


class _KotlinStdin:
    def __init__(self, source):
        self.source = source

    def readline(self, *args):
        line = self.source.readLine()
        if line is None:
            return ""
        return line + "\n"

    def read(self, *args):
        return self.readline()


def run(code, stdout_sink, stdin_source):
    stdout = _KotlinStdout(stdout_sink)
    old_stdout, old_stderr, old_stdin = sys.stdout, sys.stderr, sys.stdin
    sys.stdout, sys.stderr, sys.stdin = stdout, stdout, _KotlinStdin(stdin_source)

    g = {"__name__": "__main__"}
    try:
        exec(compile(code, "<pytide>", "exec"), g)
    except SystemExit:
        pass
    except Exception:
        traceback.print_exc(file=stdout)
    finally:
        sys.stdout, sys.stderr, sys.stdin = old_stdout, old_stderr, old_stdin
