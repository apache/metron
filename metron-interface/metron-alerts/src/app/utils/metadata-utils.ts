import {ColumnMetadata} from '../model/column-metadata';

export class MetadataUtil {

  private static createColumMetaData(properties: any, columnMetadata: ColumnMetadata[], seen: string[]) {
     try {
       let columnNames = Object.keys(properties);
       for (let columnName of columnNames) {
         if (seen.indexOf(columnName) === -1) {
           seen.push(columnName);
           columnMetadata.push(
             new ColumnMetadata(columnName, (properties[columnName].type ? properties[columnName].type.toUpperCase() : ''))
           );
         }
       }
     } catch (e) {}
  }

  public static extractData(res: Response): any {
    let response: any = res || {};
    let columnMetadata = [];
    let seen: string[] = [];

    for (let index in response.metadata.indices) {
      if (index.startsWith('bro') || index.startsWith('bro') || index.startsWith('bro')) {
        let mappings = response.metadata.indices[index].mappings;
        for (let type of Object.keys(mappings)) {
          MetadataUtil.createColumMetaData(response.metadata.indices[index].mappings[type].properties, columnMetadata, seen);
        }
      }
    }

    columnMetadata.push(new ColumnMetadata('_id', 'string'));
    return columnMetadata;
  }

  public static extractESErrorMessage(error: any): any {
    let message = error.error.reason;
    error.error.root_cause.map(cause => {
      message += '\n' + cause.index + ': ' + cause.reason;
    });

    return message;
  }
}
