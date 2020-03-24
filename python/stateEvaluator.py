import subprocess
import sys

def runREPL():
  print("Score evaluator booting up", flush=True)
  for line in sys.stdin:
    if "exit" == line.rstrip():
      break
    print("Processing message: {}".format(line), flush=True, end='')
  print("Score evaluator done", flush=True)

if __name__ == "__main__":
  runREPL()
