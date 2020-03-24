import FeaturePipeline
import numpy as np

from DataLoader import DataLoader
from sklearn import svm
from sklearn.metrics import classification_report, accuracy_score

"""
  Model is a pipeline consisting of extractor, vectorizer, and classifier
"""
def trainModel(model, dataLoader):
  x_train, y_train = dataLoader.getData('train')
  fit_model = model.fit(x_train, y_train)

  x_dev, y_dev = dataLoader.getData('dev')

  predictions = fit_model.predict(x_dev)
  print("\nAccuracy: {}".format(accuracy_score(y_dev, predictions)))
  print(classification_report(y_dev, predictions, digits=3))

  # print(x_dev[0], y_dev[0])
  # probs = model.predict_proba(x_dev)
  # for probList in probs:
  #   if np.any(list(map(lambda v: v < 0.95 and v > 0.5, list(probList)))):
  #     print(probList)


  return fit_model

if __name__ == "__main__":
  dataLoader = DataLoader("tiny")
  model = FeaturePipeline.simpleFeatures(svm.SVC(probability=True));
  fit_model = trainModel(model, dataLoader)