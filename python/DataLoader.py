import json
import os
import random

from sklearn.model_selection import train_test_split

class DataLoader:
  SPLIT_RAND_STATE = 43
  DATA_PATH = "risk_AI_data/gameData"

  def __init__(self, datasetName):
    if datasetName == "tiny":
      dataset = DataLoader.loadTinyDataset()
    elif datasetName == "full":
      dataset = DataLoader.loadFullDataset()
    else:
      raise RuntimeError("Dataset {} not found".format(datasetName))

    print("Dataset {} Loaded {} games".format(datasetName, len(dataset)))

    self.splitData(dataset)

  @staticmethod
  def loadTinyDataset():
    with open(os.path.join(DataLoader.DATA_PATH, "games_0.json"), "r") as infile:
      dataset = json.load(infile)

    return dataset

  @staticmethod
  def loadFullDataset():
    gamesList = []
    files = os.listdir(DataLoader.DATA_PATH)
    # Each file contains a list of games
    for file in files:
      with open(os.path.join(DataLoader.DATA_PATH, file), "r") as infile:
        games = json.load(infile)
      gamesList.extend(games)

    return gamesList

  def getData(self, split):
    if split == "train":
      return self.train
    elif split == "dev":
      return self.dev
    elif split == "test":
      return self.test
    else:
      raise RuntimeError("Dataset split {} not found".format(split))


  """
    We split data by game first, then shuffle it within the splits to avoid the possibility
    of information leakages between splits
  """
  def splitData(self, data):
    train, rest = train_test_split(data, test_size=0.4, random_state=DataLoader.SPLIT_RAND_STATE)
    dev, test = train_test_split(data, test_size=0.5, random_state=DataLoader.SPLIT_RAND_STATE)

    self.train = DataLoader.shuffleData(train)
    self.dev = DataLoader.shuffleData(dev)
    self.test = DataLoader.shuffleData(test)

  """
    Shuffles states and splits into x,y pairs
  """
  @staticmethod
  def shuffleData(data):
    stateWinPairs = []
    for game in data:
      winnerID = game["winner"]
      for state in game["states"]:
        stateWinPairs.append((state, winnerID))

    random.shuffle(stateWinPairs)

    return tuple(zip(*stateWinPairs))
