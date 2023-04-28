import pandas as pd
import glob


#get a list of all CSV files in the directory
file_list = glob.glob('dataset/*.csv')
#create an empty list to hold the individual dataframes
df_list = []

#get to each gile in the file list
for f in file_list:
    #read the csv file and append it to the list
    df = pd.read_csv(f)
    df_list.append(df)
#merge all the dataframes into one
df_merged = pd.concat(df_list, ignore_index=True)

#remove the field you dont want 
df_merged = df_merged.drop('Unnamed: 0',axis=1)

# Rename the 'song_name' column to 'title'
df_merged = df_merged.rename(columns={'Lyric': 'Lyrics'})

#drop missing values
df_merged = df_merged.dropna()

#write the final dataset back out to a file
df_merged.to_csv('final_dataset.csv',index=False)

