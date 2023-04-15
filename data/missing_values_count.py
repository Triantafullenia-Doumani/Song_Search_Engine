import pandas as pd

# read the merged CSV file into a DataFrame
df = pd.read_csv('final_dataset.csv')

# count the number of missing values in each column
missing_values_count = df.isnull().sum()

# print the count of missing values in each column
print(missing_values_count)
