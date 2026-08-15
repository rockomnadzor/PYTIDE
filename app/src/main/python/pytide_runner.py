import sys
import io
import traceback


def run(code):
    stdout = io.StringIO()
    stderr = io.StringIO()
    old_stdout, old_stderr = sys.stdout, sys.stderr
    sys.stdout, sys.stderr = stdout, stderr

    g = {"__name__": "__main__"}
    try:
        exec(compile(code, "<pytide>", "exec"), g)
    except SystemExit:
        pass
    except Exception:
        traceback.print_exc(file=stderr)
    finally:
        sys.stdout, sys.stderr = old_stdout, old_stderr

    out = stdout.getvalue()
    err = stderr.getvalue()
    combined = out + err
    if not combined.strip():
        combined = "(нет вывода)"
    return combined
