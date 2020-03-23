import FeaturePipeline

from DataLoader import DataLoader
# from sklearn.naive_bayes import GaussianNB
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

  return fit_model

if __name__ == "__main__":
  dataLoader = DataLoader("tiny")
  model = FeaturePipeline.simpleFeatures(svm.SVC());
  fit_model = trainModel(model, dataLoader)