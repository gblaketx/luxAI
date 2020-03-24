import json
import subprocess
import sys

"""
  The stateEvaluator runs a REPL called by the Java client to evaluate game states.

  The Java client sends over a JSON-encoded state string on stdin
  The state evaluator evaluates the string according to pretrained model
  and returns the floating-point score on stdout
"""

def fakeModel(gameState):
  return float(sum(gameState["armiesOnCountry"]) + 
    sum(gameState["countryOwners"]) + 
    gameState["playerTurn"])

def loadModel():
  return fakeModel

def runREPL(model):
  for line in sys.stdin:
    if "exit" == line.rstrip():
      break
    # print("Processing message: {}".format(line), flush=True, end='')
    gameState = json.loads(line)
    print(model(gameState), flush=True)

if __name__ == "__main__":
  model = loadModel()
  runREPL(model)
