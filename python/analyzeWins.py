import os
import warnings

import numpy as np
import pandas as pd

from collections import defaultdict
from operator import itemgetter

DATA_PATH = "risk_AI_data/tournament"

"""
  directory: Directory where the data are stored
  bots: list of bots to evaluate. If not specified all bots are included
"""
def analyzeWins(directory, bots=None):
  print("Analyzing wins from {}".format(directory))

  winsData = []

  dirPath = os.path.join(DATA_PATH, directory)
  for filename in os.listdir(dirPath):
    winsData.append(pd.read_csv(os.path.join(dirPath, filename)))


  if not bots is None:
    bots = frozenset(bots)
    def filterPlayers(df):
      return len(bots.intersection(frozenset(df.Player.values))) > 0

    winsData = list(filter(filterPlayers, winsData))

  calculateTotalWinsPercentage(winsData)
  # calculateAverageWinPercentage(winsData)



def calculateTotalWinsPercentage(winsData):
  totalGamesCounts = defaultdict(int)
  winsCounts = defaultdict(int)
  for tournamentRound in winsData:
    for _, row in tournamentRound.iterrows():
      winsCounts[row.Player] += row.Wins
      totalGamesCounts[row.Player] += row.Wins + row.Losses

  if len(frozenset(totalGamesCounts.values())) > 1:
    warnings.warn("Tournament not complete: Not all players have played the same number of games")

  # TODO: Assumes that all players have played the same number of games. Verify this
  # totalGames = sum(winsCounts.values())
  winPercentages = []
  for player, wins in winsCounts.items():
    winPercentages.append((player, wins / totalGamesCounts[player]))

  winPercentages.sort(key=itemgetter(1), reverse=True)
  
  print("\nWin Percentages (Micro Average):")
  for (player, percentage) in winPercentages:
    print("{}: {:.1f}".format(player, percentage * 100))


def calculateAverageWinPercentage(winsData):
  winPercentages = defaultdict(list)
  for tournamentRound in winsData:
    for _, row in tournamentRound.iterrows():
      percentage = row.Wins / (row.Wins + row.Losses)
      winPercentages[row.Player].append(percentage)

  averageWinPercentages = []
  for player, percentages in winPercentages.items():
    averageWinPercentages.append((player, np.mean(percentages)))

  averageWinPercentages.sort(key=itemgetter(1), reverse=True)
  
  print("\nAverage Win Percentages (Macro Average):")
  for (player, percentage) in averageWinPercentages:
    print("{}: {:.1f}".format(player, percentage * 100))   

if __name__ == "__main__":
  # analyzeWins("version_0")
  analyzeWins("version_0", ["Monty_1.00"])
