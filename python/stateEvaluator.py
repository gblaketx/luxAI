import subprocess
import sys

def runREPL():
  print("Score evaluator booting up")
  for line in sys.stdin:
    if "exit" == line.rstrip():
      break
    print("Processing message: {}".format(line))
  print("Score evaluator done")

if __name__ == "__main__":
  runREPL()
