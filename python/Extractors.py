from sklearn.base import BaseEstimator, TransformerMixin

class SimpleExtractor(BaseEstimator, TransformerMixin):

  # TODO: See if this can be done Javaside
  PHASE_MAP = {
    "Draft": 0,
    "Reinforce": 1,
    "Attack": 2,
    "Fortify": 3
  }

  def __init__(self):
    print("Simple Feature Extractor", end=" ")


  def transform(self, examples, y=None):
    return [SimpleExtractor.extractFeaturesFromState(state) for state in examples]

  def fit(self, examples, y=None):
    return self

  @staticmethod
  def extractFeaturesFromState(state):
    features = {
      "phase": SimpleExtractor.PHASE_MAP[state["phase"]],
      "playerTurn": state["playerTurn"]
    }
    armyFeatures = { "army_{}".format(i): count for i, count in enumerate(state["armiesOnCountry"]) }
    countryOwnerFeatures = { "country_{}".format(i): owner for i, owner in enumerate(state["countryOwners"])}
    features.update(armyFeatures)
    features.update(countryOwnerFeatures)
    return features