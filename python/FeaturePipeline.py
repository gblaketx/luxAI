import Extractors

from sklearn.pipeline import Pipeline
from sklearn.feature_extraction import DictVectorizer

def simpleFeatures(model):
  return Pipeline([
      ('feats', Extractors.SimpleExtractor()),
      ('vect', DictVectorizer()),
      ('clf', model)
    ])
