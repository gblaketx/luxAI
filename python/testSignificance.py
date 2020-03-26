import os
import statistics

import pandas as pd

from scipy.stats import ttest_ind

DATA_DIR = "risk_AI_data/tournament"

def readData(filename, playerID):
  filepath = os.path.join(DATA_DIR, filename)
  df = pd.read_csv(filepath)

  print("\nData: {}".format(filename))
  print("Players: {}".format(df.Player.values))
  playerRows = df.loc[df.Player == playerID]
  numWins = playerRows.Wins.values[0]
  numLosses = playerRows.Losses.values[0]
  meanWins = numWins / (numWins + numLosses)
  data = [1] * numWins + [0] * numLosses

  print("{} proportion wins: {:.3f}".format(playerID, meanWins))
  print("{} STDEV wins: {:.3f}".format(playerID, statistics.stdev(data)))  

  return data

def testSignificance(file1, file2, playerID_1, playerID_2):
  data1 = readData(file1, playerID_1)
  data2 = readData(file2, playerID_2)
  stat, p = ttest_ind(data1, data2)

  print("\np = {:.3f}".format(p))
  if p < 0.05:
    print("*", end="")
    if p < 0.01: print("*", end="")
    if p < 0.001: print("*", end="")
    print("Significant difference between {} and {}".format(file1, file2))
  else:
    print("No significant difference detected between {} and {}".format(file1, file2))

if __name__ == "__main__":
  testSignificance("wins_0.csv", "wins_2.csv", "Monty_1.00", "Monty_1.01")